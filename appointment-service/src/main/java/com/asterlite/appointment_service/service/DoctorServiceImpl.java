package com.asterlite.appointment_service.service;

import com.asterlite.appointment_service.dto.DoctorRequest;
import com.asterlite.appointment_service.dto.DoctorResponse;
import com.asterlite.appointment_service.entity.Doctor;
import com.asterlite.appointment_service.mapper.DoctorMapper;
import com.asterlite.appointment_service.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository repository;
    private final DoctorMapper mapper;

    @Override
    public DoctorResponse create(DoctorRequest request) {
        Doctor doctor = mapper.toEntity(request);
        // save() returns the entity with the generated id; the DTO goes out, never the entity (criterion 4).
        return mapper.toResponse(repository.save(doctor));
    }
}
