package com.asterlite.patient_service.mapper;

import com.asterlite.patient_service.dto.PatientRequest;
import com.asterlite.patient_service.dto.PatientResponse;
import com.asterlite.patient_service.entity.Patient;
import org.springframework.stereotype.Component;

@Component
public class PatientMapper {

    public Patient toEntity(PatientRequest request){
        Patient patient = new Patient();
        patient.setFirstName(request.firstName());
        patient.setLastName(request.lastName());
        patient.setEmail(request.email());
        patient.setPhone(request.phone());
        patient.setDateOfBirth(request.dateOfBirth());
        patient.setPreferredChannel(request.preferredChannel());
        return patient;
    }

    public PatientResponse toResponse(Patient patient){
        return new PatientResponse(
                patient.getId(),
                patient.getFirstName(),
                patient.getLastName(),
                patient.getEmail(),
                patient.getPhone(),
                patient.getDateOfBirth(),
                patient.getPreferredChannel(),
                patient.getStatus(),
                patient.getCreatedAt(),
                patient.getUpdatedAt()
        );
    }
}
