package com.asterlite.appointment_service.service;

import com.asterlite.appointment_service.dto.AppointmentRequest;
import com.asterlite.appointment_service.dto.AppointmentResponse;
import com.asterlite.appointment_service.entity.Appointment;
import com.asterlite.appointment_service.entity.Doctor;
import com.asterlite.appointment_service.entity.Slot;
import com.asterlite.appointment_service.enums.AppointmentStatus;
import com.asterlite.appointment_service.enums.SlotStatus;
import com.asterlite.appointment_service.exception.SlotAlreadyBookedException;
import com.asterlite.appointment_service.mapper.AppointmentMapper;
import com.asterlite.appointment_service.repository.AppointmentRepository;
import com.asterlite.appointment_service.repository.SlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Plain Mockito, no Spring context. @Transactional does nothing here: this proves the logic, not the rollback.
@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {

    private static final Long PATIENT_ID = 1L;
    private static final Long SLOT_ID = 7L;
    private static final Long DOCTOR_ID = 2L;

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private SlotRepository slotRepository;

    private AppointmentServiceImpl service;

    @BeforeEach
    void setUp() {
        // The mapper has no dependencies, so the real one is used instead of a mock.
        service = new AppointmentServiceImpl(appointmentRepository, slotRepository, new AppointmentMapper());
    }

    @Test
    void book_freeSlot_savesPendingAppointmentAndBooksSlot() {
        Slot slot = slot(SlotStatus.AVAILABLE);
        when(slotRepository.findById(SLOT_ID)).thenReturn(Optional.of(slot));
        // save() returns its argument, as the real repository does.
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse response = service.book(new AppointmentRequest(PATIENT_ID, SLOT_ID));

        // The slot was changed and written.
        assertThat(slot.getStatus()).isEqualTo(SlotStatus.BOOKED);
        verify(slotRepository).saveAndFlush(slot);

        // The appointment that reached the repository.
        ArgumentCaptor<Appointment> saved = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(saved.getValue().getPatientId()).isEqualTo(PATIENT_ID);
        assertThat(saved.getValue().getSlot()).isSameAs(slot);

        assertThat(response.status()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(response.slotId()).isEqualTo(SLOT_ID);
        assertThat(response.doctorId()).isEqualTo(DOCTOR_ID);
    }

    @Test
    void book_bookedSlot_throwsConflictAndNeverSavesAppointment() {
        when(slotRepository.findById(SLOT_ID)).thenReturn(Optional.of(slot(SlotStatus.BOOKED)));

        assertThatThrownBy(() -> service.book(new AppointmentRequest(PATIENT_ID, SLOT_ID)))
                .isInstanceOf(SlotAlreadyBookedException.class)
                // The message names the slot.
                .hasMessageContaining(String.valueOf(SLOT_ID));

        verify(appointmentRepository, never()).save(any());
        verify(slotRepository, never()).saveAndFlush(any());
    }

    private Slot slot(SlotStatus status) {
        Doctor doctor = new Doctor();
        doctor.setId(DOCTOR_ID);

        Instant start = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Slot slot = new Slot();
        slot.setId(SLOT_ID);
        slot.setDoctor(doctor);
        slot.setStartTime(start);
        slot.setEndTime(start.plus(30, ChronoUnit.MINUTES));
        slot.setStatus(status);
        return slot;
    }
}
