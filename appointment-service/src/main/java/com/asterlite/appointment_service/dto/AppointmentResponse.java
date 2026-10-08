package com.asterlite.appointment_service.dto;

import com.asterlite.appointment_service.enums.AppointmentStatus;

import java.time.Instant;

// Flat shape from the API contract: the slot's doctor and times are copied in, not nested.
public record AppointmentResponse(
        Long id,
        Long patientId,
        Long slotId,
        Long doctorId,
        Instant startTime,
        Instant endTime,
        AppointmentStatus status,
        Instant createdAt,
        Instant updatedAt) {
}
