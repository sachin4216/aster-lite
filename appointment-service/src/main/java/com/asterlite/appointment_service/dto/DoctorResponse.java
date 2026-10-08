package com.asterlite.appointment_service.dto;

public record DoctorResponse(
        Long id,
        String name,
        String specialty) {
}
