package com.asterlite.appointment_service.client;

import com.asterlite.appointment_service.exception.PatientNotFoundException;
import com.asterlite.appointment_service.exception.PatientServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

// The only class that knows patient-service is reached over HTTP.
// The rest of the service calls getPatient and never sees a URL.
@Component
@RequiredArgsConstructor
@Slf4j
public class PatientClient {

    // Name of the retry and breaker instances in appointment-service.yml.
    private static final String PATIENT_SERVICE = "patientService";

    // The bean from RestClientConfig, already pointed at http://patient-service.
    private final RestClient patientRestClient;

    // Order at runtime: Retry( CircuitBreaker( HTTP call ) ). Retry is the outer one,
    // so the breaker records every attempt, and the fallback runs once, after the last attempt.
    // The fallback is on @Retry only: on @CircuitBreaker it would replace the exception
    // before Retry sees it, and Retry would never retry.
    @Retry(name = PATIENT_SERVICE, fallbackMethod = "unavailable")
    @CircuitBreaker(name = PATIENT_SERVICE)
    public PatientSummary getPatient(Long id) {
        return patientRestClient.get()
                .uri("/api/patients/{id}", id)
                .retrieve()
                // patient-service answers 404 for an unknown patient. Turn that into this service's own exception.
                // An INACTIVE patient arrives as 200, so the caller checks the status itself.
                .onStatus(status -> status.value() == 404, (request, response) -> {
                    throw new PatientNotFoundException(id);
                })
                .body(PatientSummary.class);
    }

    // Fallback 1: every attempt failed (timeout, connection refused, 5xx).
    // Resilience4j picks the fallback by exception type. PatientNotFoundException matches
    // neither fallback, so it leaves this class unchanged and still becomes a 404.
    private PatientSummary unavailable(Long id, RestClientException ex) {
        log.warn("patient-service call failed for patient {}: {}", id, ex.toString());
        throw new PatientServiceUnavailableException(ex);
    }

    // Fallback 2: the breaker is OPEN, no HTTP call was made.
    private PatientSummary unavailable(Long id, CallNotPermittedException ex) {
        log.warn("patient-service call skipped for patient {}: {}", id, ex.getMessage());
        throw new PatientServiceUnavailableException(ex);
    }
}
