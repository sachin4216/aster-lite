package com.asterlite.appointment_service.mapper;

import com.asterlite.appointment_service.dto.DoctorRequest;
import com.asterlite.appointment_service.dto.DoctorResponse;
import com.asterlite.appointment_service.entity.Doctor;
import org.springframework.stereotype.Component;

@Component
public class DoctorMapper {

    public Doctor toEntity(DoctorRequest request){
        Doctor doctor = new Doctor();
        doctor.setName(request.name());
        doctor.setSpecialty(request.specialty());
        return doctor;
    }

    public DoctorResponse toResponse(Doctor doctor){
        return new DoctorResponse(
                doctor.getId(),
                doctor.getName(),
                doctor.getSpecialty()
        );
    }
}
