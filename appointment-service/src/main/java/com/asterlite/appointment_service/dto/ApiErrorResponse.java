package com.asterlite.appointment_service.dto;

import java.time.Instant;
import java.util.List;

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
