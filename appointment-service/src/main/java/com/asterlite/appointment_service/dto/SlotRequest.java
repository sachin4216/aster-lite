package com.asterlite.appointment_service.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

// No doctor id here: it comes from the URL. No status either: the server sets it.
public record SlotRequest(
        // @Future skips null, so @NotNull is needed as well.
        @NotNull
        @Future
        Instant startTime,
        @NotNull
        @Future
        Instant endTime) {
}
