package com.asterlite.patient_service.controller;

import com.asterlite.patient_service.dto.PageResponse;
import com.asterlite.patient_service.dto.PatientRequest;
import com.asterlite.patient_service.dto.PatientResponse;
import com.asterlite.patient_service.enums.PatientStatus;
import com.asterlite.patient_service.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.Set;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {
    // Only these fields may be used in ?sort=. Anything else is a client mistake, not a server error.
    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "firstName", "lastName", "email", "dateOfBirth", "status", "createdAt", "updatedAt");

    private final PatientService service;

    // The cap comes from the Config Server file, not from code. Not final, so Lombok leaves it out of the constructor.
    @Value("${spring.data.web.pageable.max-page-size:100}")
    private int maxPageSize;

    @PostMapping
    public ResponseEntity<PatientResponse> register(@Valid @RequestBody PatientRequest request){
        PatientResponse created = service.register(request);
        // created(uri) sets status 201 and the Location header. A relative URI still works behind the gateway.
        URI location = URI.create("/api/patients/" + created.id());
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PatientResponse> getById(@PathVariable Long id){
        return ResponseEntity.ok().body(service.getById(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<PatientResponse>> list(
            // required = false: the filter is optional, the parameter is null when absent.
            // Spring converts "ACTIVE" to the enum; an unknown value becomes a 400 through handleTypeMismatch.
            @RequestParam(required = false) PatientStatus status,
            @RequestParam(required = false) String lastName,
            // Explicit int parameters instead of Pageable: "abc" cannot be converted to int,
            // so Spring raises a type mismatch and the client gets a 400 instead of a silent default.
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            // Spring still parses ?sort=field,direction. Default order is id ascending.
            @SortDefault(sort = "id") Sort sort){

        if (page < 0) {
            // ResponseStatusException carries its own status; the catch-all handler turns it into ApiErrorResponse.
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be 0 or greater");
        }
        if (size < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size must be 1 or greater");
        }
        // Without this check, ?sort=unknown fails inside the query and comes back as a 500.
        sort.forEach(order -> {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot sort by '" + order.getProperty() + "'");
            }
        });

        // A size above the cap is reduced, not rejected: the response shows the size actually used.
        Pageable pageable = PageRequest.of(page, Math.min(size, maxPageSize), sort);
        return ResponseEntity.ok(service.list(status, lastName, pageable));
    }

    // Same PatientRequest and @Valid as POST, so the PAT-1 validation rules apply unchanged.
    @PutMapping("/{id}")
    public ResponseEntity<PatientResponse> update(@PathVariable Long id, @Valid @RequestBody PatientRequest request){
        return ResponseEntity.ok(service.update(id, request));
    }
}
