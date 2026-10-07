package com.asterlite.patient_service.repository;

import com.asterlite.patient_service.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

// Spring Data generates the implementation at startup. save(), findById() etc. are inherited.
public interface PatientRepository extends JpaRepository<Patient, Long> {
    // Derived query: Spring Data builds the SQL from the method name.
    boolean existsByEmail(String email);
}
