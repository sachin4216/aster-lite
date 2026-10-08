package com.asterlite.patient_service.service;

import com.asterlite.patient_service.dto.PageResponse;
import com.asterlite.patient_service.dto.PatientRequest;
import com.asterlite.patient_service.dto.PatientResponse;
import com.asterlite.patient_service.enums.PatientStatus;
import org.springframework.data.domain.Pageable;

public interface PatientService {
    PatientResponse register(PatientRequest request);
    PatientResponse getById(Long id);
    // status and lastName are optional filters; pass null to skip one.
    PageResponse<PatientResponse> list(PatientStatus status, String lastName, Pageable pageable);
    PatientResponse update(Long id, PatientRequest request);
    // Soft delete: sets the status to INACTIVE, the row stays.
    void deactivate(Long id);
}
