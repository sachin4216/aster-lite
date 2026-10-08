package com.asterlite.appointment_service.service;

import com.asterlite.appointment_service.dto.AppointmentRequest;
import com.asterlite.appointment_service.dto.AppointmentResponse;

public interface AppointmentService {
    AppointmentResponse book(AppointmentRequest request);
    AppointmentResponse getById(Long id);
}
