package com.asterlite.appointment_service.entity;

import com.asterlite.appointment_service.enums.SlotStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "slots")
@Getter
@Setter
@NoArgsConstructor
public class Slot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many slots belong to one doctor. The slots table gets a doctor_id foreign key column.
    // LAZY: loading a slot does not also SELECT the doctor unless you read a doctor field.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(nullable = false)
    private Instant startTime;

    @Column(nullable = false)
    private Instant endTime;

    // STRING stores "AVAILABLE"/"BOOKED". The default ORDINAL stores 0/1 and breaks if the enum is reordered.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SlotStatus status;

    // Hibernate adds "where version = ?" to every UPDATE and increases the number by one.
    // Nothing relies on it until APT-7, and it is never copied into a response (criterion 8).
    @Version
    private Long version;
}
