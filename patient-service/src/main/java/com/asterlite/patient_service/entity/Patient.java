package com.asterlite.patient_service.entity;

import com.asterlite.patient_service.enums.NotificationChannel;
import com.asterlite.patient_service.enums.PatientStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
// The unique constraint lives in the database: only MySQL can stop two simultaneous inserts.
@Table(name = "patients", uniqueConstraints = @UniqueConstraint(name="uk_patients_email", columnNames = "email"))
// Lombok generates getters, setters and the no-args constructor that JPA requires.
@Getter
@Setter
@NoArgsConstructor
public class Patient {
    @Id
    // IDENTITY uses MySQL AUTO_INCREMENT, so the INSERT runs as soon as save() is called.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String firstName;

    @Column(nullable = false, length = 50)
    private String lastName;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 10)
    private String phone;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    // STRING stores "EMAIL"/"SMS". The default ORDINAL stores 0/1 and breaks if the enum is reordered.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel preferredChannel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PatientStatus status;

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

    // JPA calls this just before every UPDATE (used from PAT-4).
    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
