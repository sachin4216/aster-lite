package com.asterlite.appointment_service.repository;

import com.asterlite.appointment_service.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // Derived query: select a from Appointment a where a.slot.id = ?
    List<Appointment> findBySlotId(Long slotId);
}
