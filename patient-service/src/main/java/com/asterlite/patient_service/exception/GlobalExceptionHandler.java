package com.asterlite.patient_service.exception;

import com.asterlite.patient_service.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

// One place that turns exceptions from any controller into HTTP responses.
@RestControllerAdvice
public class GlobalExceptionHandler {
    // Thrown by Spring when @Valid fails.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiErrorResponse.FieldViolation> errors = ex.getBindingResult().getFieldErrors().stream().map(error -> new ApiErrorResponse.FieldViolation(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(ApiErrorResponse.FieldViolation::field)).toList();

        String message = errors.size() == 1 ? "1 field is invalid" : errors.size() + " fields are invalid";
        return build(HttpStatus.BAD_REQUEST, message, request, errors);
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateEmail(DuplicateEmailException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    // Builds every error the same way, so all responses have identical fields.
    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String message,
                                                   HttpServletRequest request, List<ApiErrorResponse.FieldViolation> errors) {
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                // Standard text for the status, for example "Bad Request" or "Conflict".
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                errors);
        // ResponseEntity sets the real HTTP status; the body's "status" field is only a copy.
        return ResponseEntity.status(status).body(body);
    }
}
