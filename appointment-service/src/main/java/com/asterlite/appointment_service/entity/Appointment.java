package com.asterlite.appointment_service.entity;

import com.asterlite.appointment_service.enums.AppointmentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // A plain number, not a relationship: the patient lives in patient-service's database,
    // so MySQL cannot enforce a foreign key here.
    @Column(nullable = false)
    private Long patientId;

    // ManyToOne, not OneToOne: after a CANCELLED appointment the same slot can be booked again.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slot_id", nullable = false)
    private Slot slot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;

    // updatable = false: Hibernate never includes this column in an UPDATE.
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    // JPA calls this just before the INSERT, so the server sets both timestamps.
    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    // JPA calls this just before every UPDATE (used on Day 3, when payment changes the status).
    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
