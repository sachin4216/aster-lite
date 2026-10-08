package com.asterlite.appointment_service.repository;

import com.asterlite.appointment_service.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

// Spring Data generates the implementation at startup. save(), findById() etc. are inherited.
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
}
