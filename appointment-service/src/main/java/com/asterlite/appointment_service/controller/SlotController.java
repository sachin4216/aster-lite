package com.asterlite.appointment_service.controller;

import com.asterlite.appointment_service.dto.PageResponse;
import com.asterlite.appointment_service.dto.SlotRequest;
import com.asterlite.appointment_service.dto.SlotResponse;
import com.asterlite.appointment_service.enums.SlotStatus;
import com.asterlite.appointment_service.service.SlotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
// {doctorId} in the class-level mapping is available to every method as a @PathVariable.
@RequestMapping("/api/doctors/{doctorId}/slots")
@RequiredArgsConstructor
public class SlotController {

    private final SlotService service;

    // Same property and default as patient-service. Not final, so Lombok leaves it out of the constructor.
    @Value("${spring.data.web.pageable.max-page-size:100}")
    private int maxPageSize;

    @PostMapping
    public ResponseEntity<SlotResponse> create(@PathVariable Long doctorId,
                                               @Valid @RequestBody SlotRequest request){
        // The contract asks for 201 and the slot, with no Location header: there is no GET for one slot.
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(doctorId, request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<SlotResponse>> list(
            @PathVariable Long doctorId,
            // Spring converts "AVAILABLE" to the enum; an unknown value becomes a 400 through handleTypeMismatch.
            @RequestParam(required = false) SlotStatus status,
            // Explicit int parameters instead of Pageable: "abc" cannot be converted to int,
            // so Spring raises a type mismatch and the client gets a 400 instead of a silent default.
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size){

        if (page < 0) {
            // ResponseStatusException carries its own status; the catch-all handler turns it into ApiErrorResponse.
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be 0 or greater");
        }
        if (size < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size must be 1 or greater");
        }

        // The order is fixed by the story (criterion 5), so there is no ?sort= parameter.
        // A size above the cap is reduced, not rejected: the response shows the size actually used.
        Pageable pageable = PageRequest.of(page, Math.min(size, maxPageSize), Sort.by("startTime").ascending());
        return ResponseEntity.ok(service.list(doctorId, status, pageable));
    }
}
