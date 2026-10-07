package com.asterlite.patient_service.service;

import com.asterlite.patient_service.dto.PatientRequest;
import com.asterlite.patient_service.dto.PatientResponse;

public interface PatientService {
    PatientResponse register(PatientRequest request);
}
