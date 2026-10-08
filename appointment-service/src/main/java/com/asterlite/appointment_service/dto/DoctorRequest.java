package com.asterlite.appointment_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// No id field: the client cannot set it, and an "id" in the JSON is ignored (criterion 3).
public record DoctorRequest(
        // @NotNull + @Size instead of @NotBlank + @Size: a blank name then produces ONE error, not two.
        @NotNull
        @Size(min = 2, max = 100)
        String name,
        @NotNull
        @Size(min = 2, max = 50)
        String specialty) {

    // Compact constructor: runs when Jackson builds the record, BEFORE validation.
    // Trimming here means "   " becomes "" and fails @Size.
    public DoctorRequest {
        name = trim(name);
        specialty = trim(specialty);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
