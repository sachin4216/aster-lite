package com.asterlite.appointment_service.service;

import com.asterlite.appointment_service.dto.DoctorRequest;
import com.asterlite.appointment_service.dto.DoctorResponse;

public interface DoctorService {
    DoctorResponse create(DoctorRequest request);
}
