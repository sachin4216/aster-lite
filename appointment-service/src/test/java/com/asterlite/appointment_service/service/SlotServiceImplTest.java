package com.asterlite.appointment_service.service;

import com.asterlite.appointment_service.dto.SlotRequest;
import com.asterlite.appointment_service.dto.SlotResponse;
import com.asterlite.appointment_service.entity.Doctor;
import com.asterlite.appointment_service.entity.Slot;
import com.asterlite.appointment_service.enums.SlotStatus;
import com.asterlite.appointment_service.exception.DoctorNotFoundException;
import com.asterlite.appointment_service.exception.SlotOverlapException;
import com.asterlite.appointment_service.mapper.SlotMapper;
import com.asterlite.appointment_service.repository.DoctorRepository;
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

// Plain Mockito, no Spring context: the test starts in milliseconds and needs no database.
@ExtendWith(MockitoExtension.class)
class SlotServiceImplTest {

    private static final Long DOCTOR_ID = 1L;

    @Mock
    private SlotRepository slotRepository;
    @Mock
    private DoctorRepository doctorRepository;

    private SlotServiceImpl service;

    // A fixed base time in the future, so the test does not depend on the clock.
    private final Instant nine = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
    private final Instant halfPastNine = nine.plus(30, ChronoUnit.MINUTES);

    @BeforeEach
    void setUp() {
        // The mapper has no dependencies, so the real one is used instead of a mock.
        service = new SlotServiceImpl(slotRepository, doctorRepository, new SlotMapper());
    }

    @Test
    void create_unknownDoctor_throwsNotFound() {
        when(doctorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(99L, new SlotRequest(nine, halfPastNine)))
                .isInstanceOf(DoctorNotFoundException.class)
                .hasMessageContaining("99");

        verify(slotRepository, never()).save(any());
    }

    @Test
    void create_overlappingSlot_throwsConflict() {
        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor()));
        // The repository reports an existing slot that overlaps 09:00-09:30.
        when(slotRepository.existsByDoctorIdAndStartTimeBeforeAndEndTimeAfter(DOCTOR_ID, halfPastNine, nine))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(DOCTOR_ID, new SlotRequest(nine, halfPastNine)))
                .isInstanceOf(SlotOverlapException.class);

        verify(slotRepository, never()).save(any());
    }

    @Test
    void create_backToBackSlot_isSaved() {
        // An existing slot ends at 09:30; the new one is 09:30-10:00.
        Instant ten = halfPastNine.plus(30, ChronoUnit.MINUTES);
        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor()));
        // The service must ask with (new end, new start). The strict < and > live in the query itself,
        // so for a back-to-back slot the database answers false.
        when(slotRepository.existsByDoctorIdAndStartTimeBeforeAndEndTimeAfter(DOCTOR_ID, ten, halfPastNine))
                .thenReturn(false);
        // save() returns its argument, as the real repository does.
        when(slotRepository.save(any(Slot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SlotResponse response = service.create(DOCTOR_ID, new SlotRequest(halfPastNine, ten));

        ArgumentCaptor<Slot> saved = ArgumentCaptor.forClass(Slot.class);
        verify(slotRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(SlotStatus.AVAILABLE);
        assertThat(response.doctorId()).isEqualTo(DOCTOR_ID);
        assertThat(response.startTime()).isEqualTo(halfPastNine);
    }

    private Doctor doctor() {
        Doctor doctor = new Doctor();
        doctor.setId(DOCTOR_ID);
        doctor.setName("Dr. Meera Joshi");
        doctor.setSpecialty("Cardiology");
        return doctor;
    }
}
