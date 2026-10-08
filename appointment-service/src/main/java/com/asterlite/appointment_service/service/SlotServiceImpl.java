package com.asterlite.appointment_service.service;

import com.asterlite.appointment_service.dto.PageResponse;
import com.asterlite.appointment_service.dto.SlotRequest;
import com.asterlite.appointment_service.dto.SlotResponse;
import com.asterlite.appointment_service.entity.Doctor;
import com.asterlite.appointment_service.entity.Slot;
import com.asterlite.appointment_service.enums.SlotStatus;
import com.asterlite.appointment_service.exception.DoctorNotFoundException;
import com.asterlite.appointment_service.exception.InvalidSlotTimeException;
import com.asterlite.appointment_service.exception.SlotOverlapException;
import com.asterlite.appointment_service.mapper.SlotMapper;
import com.asterlite.appointment_service.repository.DoctorRepository;
import com.asterlite.appointment_service.repository.SlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SlotServiceImpl implements SlotService {

    private final SlotRepository slotRepository;
    private final DoctorRepository doctorRepository;
    private final SlotMapper mapper;

    @Override
    public SlotResponse create(Long doctorId, SlotRequest request) {
        // Load first: an unknown doctor is a 404 before anything else is checked.
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new DoctorNotFoundException(doctorId));

        // Two fields compared with each other: no single-field annotation can express this.
        // "not after" also rejects an end equal to the start (a slot of zero length).
        if (!request.endTime().isAfter(request.startTime())) {
            throw new InvalidSlotTimeException("endTime must be after startTime");
        }

        // New end first, new start second: existing.start < new.end and existing.end > new.start.
        if (slotRepository.existsByDoctorIdAndStartTimeBeforeAndEndTimeAfter(
                doctorId, request.endTime(), request.startTime())) {
            throw new SlotOverlapException(doctorId);
        }

        Slot slot = mapper.toEntity(request, doctor);
        // Server-controlled value.
        slot.setStatus(SlotStatus.AVAILABLE);
        return mapper.toResponse(slotRepository.save(slot));
    }

    @Override
    public PageResponse<SlotResponse> list(Long doctorId, SlotStatus status, Pageable pageable) {
        // Without this check an unknown doctor would return an empty page with 200 instead of a 404.
        if (!doctorRepository.existsById(doctorId)) {
            throw new DoctorNotFoundException(doctorId);
        }

        // Page.map converts each entity to a DTO and keeps the paging numbers.
        Page<SlotResponse> page = slotRepository.findByDoctor(doctorId, status, pageable).map(mapper::toResponse);
        return PageResponse.from(page);
    }
}
