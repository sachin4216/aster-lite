package com.asterlite.appointment_service.service;

import com.asterlite.appointment_service.client.PatientClient;
import com.asterlite.appointment_service.client.PatientSummary;
import com.asterlite.appointment_service.dto.AppointmentRequest;
import com.asterlite.appointment_service.entity.Doctor;
import com.asterlite.appointment_service.entity.Slot;
import com.asterlite.appointment_service.enums.SlotStatus;
import com.asterlite.appointment_service.exception.SlotAlreadyBookedException;
import com.asterlite.appointment_service.repository.AppointmentRepository;
import com.asterlite.appointment_service.repository.DoctorRepository;
import com.asterlite.appointment_service.repository.SlotRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

// Full application context and the real MySQL: optimistic locking is decided by the database,
// so a mocked repository could not prove anything here.
// Needs Docker, discovery-server and config-server running. patient-service is not needed.
@SpringBootTest
class AppointmentBookingConcurrencyTest {

    private static final int THREADS = 50;

    @Autowired
    private AppointmentService appointmentService;
    @Autowired
    private DoctorRepository doctorRepository;
    @Autowired
    private SlotRepository slotRepository;
    @Autowired
    private AppointmentRepository appointmentRepository;

    // Replaces the real PatientClient bean, so no HTTP call, retry or circuit breaker is involved.
    @MockitoBean
    private PatientClient patientClient;

    private Doctor doctor;
    private Slot slot;

    @BeforeEach
    void setUp() {
        // Every patient id is an active patient.
        when(patientClient.getPatient(anyLong())).thenAnswer(invocation -> new PatientSummary(
                invocation.getArgument(0), "Asha", "Rao", "asha@example.com", "9876543210", "EMAIL", "ACTIVE"));

        doctor = new Doctor();
        doctor.setName("Dr Concurrency Test");
        doctor.setSpecialty("Testing");
        doctor = doctorRepository.save(doctor);

        Instant start = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        slot = new Slot();
        slot.setDoctor(doctor);
        slot.setStartTime(start);
        slot.setEndTime(start.plus(30, ChronoUnit.MINUTES));
        slot.setStatus(SlotStatus.AVAILABLE);
        slot = slotRepository.save(slot);
    }

    // The test writes to the real appointment_db, so it removes its own rows. Children first.
    @AfterEach
    void cleanUp() {
        appointmentRepository.deleteAll(appointmentRepository.findBySlotId(slot.getId()));
        slotRepository.deleteById(slot.getId());
        doctorRepository.deleteById(doctor.getId());
    }

    @Test
    void book_fiftyConcurrentRequestsForOneSlot_exactlyOneSucceeds() throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        // ready: every thread reports that it is waiting. start: the gate that releases them together.
        // done: every thread reports that it has finished.
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREADS);

        AtomicInteger booked = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();
        // Anything that is neither a success nor a conflict, for example a 500. Must stay empty.
        Queue<Throwable> unexpected = new ConcurrentLinkedQueue<>();

        for (int i = 1; i <= THREADS; i++) {
            // 50 different patients, one slot.
            long patientId = i;
            pool.submit(() -> {
                try {
                    ready.countDown();
                    // All 50 threads stop here until the gate opens.
                    start.await();
                    appointmentService.book(new AppointmentRequest(patientId, slot.getId()));
                    booked.incrementAndGet();
                } catch (SlotAlreadyBookedException ex) {
                    // The exception GlobalExceptionHandler turns into a 409.
                    conflicts.incrementAndGet();
                } catch (Throwable ex) {
                    unexpected.add(ex);
                } finally {
                    done.countDown();
                }
            });
        }

        // Wait until all 50 are at the gate, then open it.
        ready.await();
        start.countDown();
        boolean finished = done.await(60, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(finished).isTrue();
        assertThat(unexpected).isEmpty();
        assertThat(booked.get()).isEqualTo(1);
        assertThat(conflicts.get()).isEqualTo(THREADS - 1);

        // The database agrees: the slot is BOOKED and has exactly one appointment.
        assertThat(slotRepository.findById(slot.getId()).orElseThrow().getStatus()).isEqualTo(SlotStatus.BOOKED);
        assertThat(appointmentRepository.findBySlotId(slot.getId())).hasSize(1);
    }
}
