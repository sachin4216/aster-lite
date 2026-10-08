package com.asterlite.patient_service.service;

import com.asterlite.patient_service.dto.PageResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Override
    public PageResponse<PatientResponse> list(PatientStatus status, String lastName, Pageable pageable) {
        // "?lastName=" arrives as an empty string. Treat it as "no filter", not "starts with nothing".
        String prefix = (lastName == null || lastName.isBlank()) ? null : lastName.trim();

        // Page.map converts each entity to a DTO and keeps the paging numbers.
        Page<PatientResponse> page = repository.search(status, prefix, pageable).map(mapper::toResponse);
        return PageResponse.from(page);
    }

    @Override
    public PatientResponse update(Long id, PatientRequest request) {
        // Load first: an unknown id is a 404 before anything else is checked.
        Patient patient = repository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException(id));

        String email = request.email().toLowerCase(Locale.ROOT);

        // Only another patient's email is a conflict. Keeping your own email is allowed.
        if (repository.existsByEmailAndIdNot(email, id)) {
            throw new DuplicateEmailException(email);
        }

        // Only client-editable fields are copied. id, status and createdAt are never touched.
        patient.setFirstName(request.firstName());
        patient.setLastName(request.lastName());
        patient.setEmail(email);
        patient.setPhone(request.phone());
        patient.setDateOfBirth(request.dateOfBirth());
        // Left out of the request: keep the current channel instead of silently resetting it to EMAIL.
        if (request.preferredChannel() != null) {
            patient.setPreferredChannel(request.preferredChannel());
        }

        try {
            // saveAndFlush runs the UPDATE now, so @PreUpdate has set updatedAt before the response is built.
            return mapper.toResponse(repository.saveAndFlush(patient));
        } catch (DataIntegrityViolationException ex) {
            // Safety net: another request took this email between the check and the UPDATE.
            throw new DuplicateEmailException(email);
        }
    }
}
