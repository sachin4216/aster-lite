package com.asterlite.patient_service.dto;

import com.asterlite.patient_service.enums.NotificationChannel;
import com.asterlite.patient_service.enums.PatientStatus;

import java.time.Instant;
import java.time.LocalDate;

public record PatientResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        LocalDate dateOfBirth,
        NotificationChannel preferredChannel,
        PatientStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
