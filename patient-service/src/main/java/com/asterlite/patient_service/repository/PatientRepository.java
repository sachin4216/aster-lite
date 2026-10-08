package com.asterlite.patient_service.repository;

import com.asterlite.patient_service.entity.Patient;
import com.asterlite.patient_service.enums.PatientStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Spring Data generates the implementation at startup. save(), findById() etc. are inherited.
public interface PatientRepository extends JpaRepository<Patient, Long> {
    // Derived query: Spring Data builds the SQL from the method name.
    boolean existsByEmail(String email);

    // JPQL: written against the entity (Patient, p.lastName), not the table.
    // "(:x is null or ...)" makes each filter optional: a null parameter switches that condition off.
    // lower(...) on both sides ignores case; concat(:lastName, '%') means "starts with".
    @Query("""
            select p from Patient p
            where (:status is null or p.status = :status)
              and (:lastName is null or lower(p.lastName) like lower(concat(:lastName, '%')))
            """)
    // Pageable adds LIMIT/OFFSET and ORDER BY. Returning Page also runs a COUNT query for totalElements.
    Page<Patient> search(@Param("status") PatientStatus status,
                         @Param("lastName") String lastName,
                         Pageable pageable);
}
