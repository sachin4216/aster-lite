package com.asterlite.appointment_service.repository;

import com.asterlite.appointment_service.entity.Slot;
import com.asterlite.appointment_service.enums.SlotStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface SlotRepository extends JpaRepository<Slot, Long> {
    // Derived query for the overlap rule: existing.start < new.end AND existing.end > new.start.
    // Before/After are strict (< and >), so a slot ending at 09:30 does not overlap one starting at 09:30.
    // Mind the argument order: the NEW slot's end comes first, its start second.
    boolean existsByDoctorIdAndStartTimeBeforeAndEndTimeAfter(Long doctorId, Instant newEnd, Instant newStart);

    // "(:status is null or ...)" makes the filter optional: a null parameter switches the condition off.
    @Query("""
            select s from Slot s
            where s.doctor.id = :doctorId
              and (:status is null or s.status = :status)
            """)
    // Pageable adds LIMIT/OFFSET and ORDER BY. Returning Page also runs a COUNT query for totalElements.
    Page<Slot> findByDoctor(@Param("doctorId") Long doctorId,
                            @Param("status") SlotStatus status,
                            Pageable pageable);
}
