package com.asterlite.patient_service.dto;

import com.asterlite.patient_service.enums.NotificationChannel;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

// A record is an immutable data carrier. It has no id, status or timestamps,
// so the client cannot set them
public record PatientRequest(
        // @NotNull + @Size instead of @NotBlank + @Size: a blank name then produces ONE error, not two.
        @NotNull
        @Size(min = 2, max = 50)
        String firstName,
        @NotNull
        @Size(min = 2, max = 50)
        String lastName,
        // @Email accepts an empty string, so @NotBlank is needed as well.
        @NotBlank
        @Email
        String email,
        // @Pattern skips null, so @NotNull is needed as well.
        @NotNull
        @Pattern(regexp = "\\d{10}", message = "must be 10 digits")
        String phone,
        @NotNull
        @Past
        LocalDate dateOfBirth,
        // Optional: no annotation. The service defaults it to EMAIL.
        NotificationChannel preferredChannel) {
    // Compact constructor: runs when Jackson builds the record, BEFORE validation.
    // Trimming here means " asha@example.com " is validated as "asha@example.com".
    public PatientRequest {
        firstName = trim(firstName);
        lastName = trim(lastName);
        email = trim(email);
        phone = trim(phone);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
