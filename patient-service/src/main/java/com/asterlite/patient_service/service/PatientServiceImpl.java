package com.asterlite.patient_service.service;

import com.asterlite.patient_service.dto.PatientRequest;
import com.asterlite.patient_service.dto.PatientResponse;
import com.asterlite.patient_service.entity.Patient;
import com.asterlite.patient_service.enums.NotificationChannel;
import com.asterlite.patient_service.enums.PatientStatus;
import com.asterlite.patient_service.exception.DuplicateEmailException;
import com.asterlite.patient_service.exception.PatientNotFoundException;
import com.asterlite.patient_service.mapper.PatientMapper;
import com.asterlite.patient_service.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService{

    private final PatientRepository repository;
    private final PatientMapper mapper;

    @Override
    public PatientResponse register(PatientRequest request){
        // Lower case before the check and before saving, so Asha@X.com and asha@x.com are the same.
        // Locale.ROOT avoids locale surprises (in Turkish, "I" does not lower-case to "i").
        String email = request.email().toLowerCase(Locale.ROOT);

        // Friendly check first: gives a clear 409 in the normal case.
        if (repository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }

        Patient patient = mapper.toEntity(request);
        patient.setEmail(email);
        // Server-controlled values.
        patient.setStatus(PatientStatus.ACTIVE);
        if (patient.getPreferredChannel() == null) {
            patient.setPreferredChannel(NotificationChannel.EMAIL);
        }

        try {
            return mapper.toResponse(repository.save(patient));
        } catch (DataIntegrityViolationException ex) {
            // Safety net: two requests passed the check together and the unique constraint stopped the second.
            throw new DuplicateEmailException(email);
        }
    }

    @Override
    public PatientResponse getById(Long id) {
        // findById returns Optional<Patient>: empty when no row has this id.
        return repository.findById(id)
                // No status check: an INACTIVE patient is still returned (criterion 3).
                .map(mapper::toResponse)
                // Empty Optional -> exception -> GlobalExceptionHandler -> 404 with the id in the message.
                .orElseThrow(() -> new PatientNotFoundException(id));
    }
}
