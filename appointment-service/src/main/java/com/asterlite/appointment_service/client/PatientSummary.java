package com.asterlite.appointment_service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// This service's own view of a patient: only the fields it needs.
// patient-service sends more (dateOfBirth, createdAt, updatedAt); ignoreUnknown skips them,
// so a new field over there never breaks booking here.
@JsonIgnoreProperties(ignoreUnknown = true)
public record PatientSummary(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        // Strings, not enums: a new channel or status in patient-service must not fail the JSON parsing here.
        String preferredChannel,
        String status) {

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }
}
