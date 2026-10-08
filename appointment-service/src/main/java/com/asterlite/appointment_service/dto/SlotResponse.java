package com.asterlite.appointment_service.dto;

import com.asterlite.appointment_service.enums.SlotStatus;

import java.time.Instant;

// No version field: it is an internal locking detail (criterion 8).
public record SlotResponse(
        Long id,
        Long doctorId,
        Instant startTime,
        Instant endTime,
        SlotStatus status) {
}
