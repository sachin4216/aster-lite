package com.asterlite.appointment_service.service;

import com.asterlite.appointment_service.client.PatientClient;
import com.asterlite.appointment_service.client.PatientSummary;
import com.asterlite.appointment_service.dto.AppointmentRequest;
import com.asterlite.appointment_service.dto.AppointmentResponse;
import com.asterlite.appointment_service.entity.Appointment;
import com.asterlite.appointment_service.entity.Doctor;
import com.asterlite.appointment_service.entity.Slot;
import com.asterlite.appointment_service.enums.AppointmentStatus;
import com.asterlite.appointment_service.enums.SlotStatus;
import com.asterlite.appointment_service.exception.PatientInactiveException;
import com.asterlite.appointment_service.exception.PatientNotFoundException;
import com.asterlite.appointment_service.exception.PatientServiceUnavailableException;
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
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// Plain Mockito, no Spring context. PatientClient is mocked, so patient-service does not need to run.
@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {

    private static final Long PATIENT_ID = 1L;
    private static final Long SLOT_ID = 7L;
    private static final Long DOCTOR_ID = 2L;

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private SlotRepository slotRepository;
    @Mock
    private PatientClient patientClient;
    // A mocked transaction manager: begin, commit and rollback do nothing.
    @Mock
    private PlatformTransactionManager transactionManager;

    private AppointmentServiceImpl service;

    @BeforeEach
    void setUp() {
        // A real TransactionTemplate around the mocked manager: it simply runs the block it is given.
        // The mapper has no dependencies, so the real one is used instead of a mock.
        service = new AppointmentServiceImpl(appointmentRepository, slotRepository, new AppointmentMapper(),
                patientClient, new TransactionTemplate(transactionManager));
    }

    @Test
    void book_freeSlot_savesPendingAppointmentAndBooksSlot() {
        Slot slot = slot(SlotStatus.AVAILABLE);
        when(patientClient.getPatient(PATIENT_ID)).thenReturn(patient("ACTIVE"));
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
        when(patientClient.getPatient(PATIENT_ID)).thenReturn(patient("ACTIVE"));
        when(slotRepository.findById(SLOT_ID)).thenReturn(Optional.of(slot(SlotStatus.BOOKED)));

        assertThatThrownBy(() -> service.book(new AppointmentRequest(PATIENT_ID, SLOT_ID)))
                .isInstanceOf(SlotAlreadyBookedException.class)
                // The message names the slot.
                .hasMessageContaining(String.valueOf(SLOT_ID));

        verify(appointmentRepository, never()).save(any());
        verify(slotRepository, never()).saveAndFlush(any());
    }

    @Test
    void book_slotChangedByAnotherRequest_throwsConflictAndNeverSavesAppointment() {
        when(patientClient.getPatient(PATIENT_ID)).thenReturn(patient("ACTIVE"));
        // The slot still reads AVAILABLE, so the status check passes.
        when(slotRepository.findById(SLOT_ID)).thenReturn(Optional.of(slot(SlotStatus.AVAILABLE)));
        // What Spring throws when the UPDATE with "where version = ?" matches no row.
        when(slotRepository.saveAndFlush(any(Slot.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Slot.class, SLOT_ID));

        assertThatThrownBy(() -> service.book(new AppointmentRequest(PATIENT_ID, SLOT_ID)))
                .isInstanceOf(SlotAlreadyBookedException.class)
                .hasMessageContaining(String.valueOf(SLOT_ID));

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void book_unknownPatient_throwsNotFoundAndNeverTouchesSlot() {
        // PatientClient turns patient-service's 404 into this exception.
        when(patientClient.getPatient(99L)).thenThrow(new PatientNotFoundException(99L));

        assertThatThrownBy(() -> service.book(new AppointmentRequest(99L, SLOT_ID)))
                .isInstanceOf(PatientNotFoundException.class)
                .hasMessageContaining("99");

        // Not even a read: the patient check comes before any database work and before the transaction.
        verifyNoInteractions(slotRepository, appointmentRepository, transactionManager);
    }

    @Test
    void book_inactivePatient_throwsConflictAndNeverTouchesSlot() {
        when(patientClient.getPatient(PATIENT_ID)).thenReturn(patient("INACTIVE"));

        assertThatThrownBy(() -> service.book(new AppointmentRequest(PATIENT_ID, SLOT_ID)))
                .isInstanceOf(PatientInactiveException.class)
                .hasMessageContaining(String.valueOf(PATIENT_ID));

        verifyNoInteractions(slotRepository, appointmentRepository, transactionManager);
    }

    @Test
    void book_patientServiceUnavailable_savesNothing() {
        // PatientClient throws this after its retries fail, or at once when the breaker is open.
        when(patientClient.getPatient(PATIENT_ID))
                .thenThrow(new PatientServiceUnavailableException(new RuntimeException("connection refused")));

        assertThatThrownBy(() -> service.book(new AppointmentRequest(PATIENT_ID, SLOT_ID)))
                .isInstanceOf(PatientServiceUnavailableException.class);

        // No read, no write, no transaction: the slot stays AVAILABLE and no appointment exists.
        verifyNoInteractions(slotRepository, appointmentRepository, transactionManager);
    }

    private PatientSummary patient(String status) {
        return new PatientSummary(PATIENT_ID, "Asha", "Rao", "asha@example.com", "9876543210", "EMAIL", status);
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
