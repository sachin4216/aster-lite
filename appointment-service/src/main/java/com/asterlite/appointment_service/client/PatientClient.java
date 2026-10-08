package com.asterlite.appointment_service.client;

import com.asterlite.appointment_service.exception.PatientNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

// The only class that knows patient-service is reached over HTTP.
// The rest of the service calls getPatient and never sees a URL.
@Component
@RequiredArgsConstructor
public class PatientClient {

    // The bean from RestClientConfig, already pointed at http://patient-service.
    private final RestClient patientRestClient;

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
}
