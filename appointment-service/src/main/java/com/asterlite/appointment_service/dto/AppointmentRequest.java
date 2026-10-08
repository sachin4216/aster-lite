package com.asterlite.appointment_service.dto;

import jakarta.validation.constraints.NotNull;

// Only the two ids. status and the timestamps are set by the server.
public record AppointmentRequest(
        @NotNull
        Long patientId,
        @NotNull
        Long slotId) {
}
