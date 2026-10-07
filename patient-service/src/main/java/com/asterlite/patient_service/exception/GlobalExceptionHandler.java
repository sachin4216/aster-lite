package com.asterlite.patient_service.exception;

import com.asterlite.patient_service.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

// One place that turns exceptions from any controller into HTTP responses.
@RestControllerAdvice
@Slf4j
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

    // The body could not be parsed: broken JSON, empty body, unknown enum value, bad date.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex,
                                                                 HttpServletRequest request) {
        // Fixed text: the exception's own message contains Java class names.
        return build(HttpStatus.BAD_REQUEST, "Request body is missing or malformed", request, List.of());
    }

    @ExceptionHandler(PatientNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFoundException(PatientNotFoundException ex, HttpServletRequest request){
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    //A path or query value has the wrong type, for example /api/patients/abc.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                               HttpServletRequest request) {
        String message = "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'";
        return build(HttpStatus.BAD_REQUEST, message, request, List.of());
    }

    // Catch-all. Spring picks the most specific handler, so this runs only when none above matches.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        // Spring's own web exceptions (unknown URL -> 404, wrong method -> 405, wrong Content-Type -> 415)
        // already carry the right status. Without this check they would all become 500.
        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatus status = HttpStatus.valueOf(errorResponse.getStatusCode().value());
            return build(status, errorResponse.getBody().getDetail(), request, List.of());
        }

        // Full stack trace goes to the log only. The client gets a generic message.
        log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request, List.of());
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
