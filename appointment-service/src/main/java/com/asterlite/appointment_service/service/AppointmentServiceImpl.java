package com.asterlite.appointment_service.service;

import com.asterlite.appointment_service.client.PatientClient;
import com.asterlite.appointment_service.client.PatientSummary;
import com.asterlite.appointment_service.dto.AppointmentRequest;
import com.asterlite.appointment_service.dto.AppointmentResponse;
import com.asterlite.appointment_service.entity.Appointment;
import com.asterlite.appointment_service.entity.Slot;
import com.asterlite.appointment_service.enums.AppointmentStatus;
import com.asterlite.appointment_service.enums.SlotStatus;
import com.asterlite.appointment_service.exception.AppointmentNotFoundException;
import com.asterlite.appointment_service.exception.PatientInactiveException;
import com.asterlite.appointment_service.exception.SlotAlreadyBookedException;
import com.asterlite.appointment_service.exception.SlotNotFoundException;
import com.asterlite.appointment_service.mapper.AppointmentMapper;
import com.asterlite.appointment_service.repository.AppointmentRepository;
import com.asterlite.appointment_service.repository.SlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final SlotRepository slotRepository;
    private final AppointmentMapper mapper;
    private final PatientClient patientClient;
    // Spring Boot creates this bean. It runs a block of code inside a transaction.
    private final TransactionTemplate transactionTemplate;

    // No @Transactional here any more: the method has a part that must run OUTSIDE the transaction.
    @Override
    public AppointmentResponse book(AppointmentRequest request) {
        // 1. Remote call first, with no transaction open (criterion 8).
        //    A slow patient-service then costs a waiting thread, but not a database connection.
        //    An unknown patient throws PatientNotFoundException inside the client.
        PatientSummary patient = patientClient.getPatient(request.patientId());

        // patient-service answers 200 for a deactivated patient, so the status is checked here.
        if (!patient.isActive()) {
            throw new PatientInactiveException(request.patientId());
        }

        // 2. Only now open the transaction, for the two database writes.
        //    @Transactional on reserve() would NOT work: the annotation is applied by a proxy around
        //    the bean, and a call from book() to this.reserve() never passes through that proxy.
        //    TransactionTemplate needs no proxy: the transaction starts and ends around this one call.
        try {
            return transactionTemplate.execute(status -> reserve(request));
        } catch (ObjectOptimisticLockingFailureException ex) {
            // Two requests read the slot as AVAILABLE at the same moment. The other one updated it first,
            // so this UPDATE ("where id = ? and version = ?") matched no row (APT-7).
            // Caught here, outside execute(): the transaction has already been rolled back.
            throw new SlotAlreadyBookedException(request.slotId());
        }
    }

    // Runs inside the transaction opened by book(). If anything throws a RuntimeException,
    // MySQL rolls back both writes, so a BOOKED slot never exists without its appointment.
    private AppointmentResponse reserve(AppointmentRequest request) {
        Slot slot = slotRepository.findById(request.slotId())
                .orElseThrow(() -> new SlotNotFoundException(request.slotId()));

        // Catches a late request: it arrives after the slot was taken and reads BOOKED.
        // Two requests at the same instant both read AVAILABLE and pass this check;
        // the version column stops the second one at saveAndFlush below.
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
