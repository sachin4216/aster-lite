package com.asterlite.appointment_service.exception;

// patient-service gave no usable answer: timeout, connection refused, 5xx or open circuit breaker.
// GlobalExceptionHandler turns it into a 503.
public class PatientServiceUnavailableException extends RuntimeException {

    public PatientServiceUnavailableException(Throwable cause) {
        // Fixed text for the client. The cause keeps the technical reason for the log.
        super("Patient service is unavailable, please try again later", cause);
    }
}
