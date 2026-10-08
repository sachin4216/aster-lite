package com.asterlite.appointment_service.service;

import com.asterlite.appointment_service.dto.AppointmentRequest;
import com.asterlite.appointment_service.dto.AppointmentResponse;
import com.asterlite.appointment_service.entity.Appointment;
import com.asterlite.appointment_service.entity.Slot;
import com.asterlite.appointment_service.enums.AppointmentStatus;
import com.asterlite.appointment_service.enums.SlotStatus;
import com.asterlite.appointment_service.exception.AppointmentNotFoundException;
import com.asterlite.appointment_service.exception.SlotAlreadyBookedException;
import com.asterlite.appointment_service.exception.SlotNotFoundException;
import com.asterlite.appointment_service.mapper.AppointmentMapper;
import com.asterlite.appointment_service.repository.AppointmentRepository;
import com.asterlite.appointment_service.repository.SlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final SlotRepository slotRepository;
    private final AppointmentMapper mapper;

    // The first method in the project that writes two tables (slots and appointments).
    // @Transactional makes them one unit: if anything below throws a RuntimeException,
    // MySQL rolls back both writes, so a BOOKED slot never exists without its appointment.
    @Transactional
    @Override
    public AppointmentResponse book(AppointmentRequest request) {
        Slot slot = slotRepository.findById(request.slotId())
                .orElseThrow(() -> new SlotNotFoundException(request.slotId()));

        // Catches a request that arrives after the slot was taken.
        // Two requests at the same instant both see AVAILABLE here; APT-7 handles that case.
        if (slot.getStatus() == SlotStatus.BOOKED) {
            throw new SlotAlreadyBookedException(slot.getId());
        }

        slot.setStatus(SlotStatus.BOOKED);
        // saveAndFlush sends the UPDATE now instead of at commit, so a failed slot update
        // stops the method before the appointment is inserted.
        slotRepository.saveAndFlush(slot);

        Appointment appointment = new Appointment();
        appointment.setPatientId(request.patientId());
        appointment.setSlot(slot);
        // Server-controlled value. Payment moves it to CONFIRMED or CANCELLED on Day 3.
        appointment.setStatus(AppointmentStatus.PENDING);

        return mapper.toResponse(appointmentRepository.save(appointment));
    }

    // readOnly: no writes happen. The transaction is still needed because open-in-view is false
    // and the mapper reads the LAZY slot, which only works while the session is open.
    @Transactional(readOnly = true)
    @Override
    public AppointmentResponse getById(Long id) {
        return appointmentRepository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new AppointmentNotFoundException(id));
    }
}
