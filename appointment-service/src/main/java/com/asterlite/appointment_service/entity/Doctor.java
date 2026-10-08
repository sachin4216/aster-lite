package com.asterlite.appointment_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "doctors")
// Lombok generates getters, setters and the no-args constructor that JPA requires.
@Getter
@Setter
@NoArgsConstructor
public class Doctor {
    @Id
    // IDENTITY uses MySQL AUTO_INCREMENT, so the INSERT runs as soon as save() is called.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String specialty;
}
