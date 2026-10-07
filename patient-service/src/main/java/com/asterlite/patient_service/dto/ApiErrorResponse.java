package com.asterlite.patient_service.dto;

import java.time.Instant;
import java.util.List;

// Custom error body. Replaces ProblemDetail, so it changes the API contract in section 6.3.
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldViolation> errors) {

    // One entry per invalid field. Empty list for non-validation errors.
    public record FieldViolation(String field, String message){}
}
