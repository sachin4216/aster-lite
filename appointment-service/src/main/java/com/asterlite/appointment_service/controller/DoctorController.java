package com.asterlite.appointment_service.controller;

import com.asterlite.appointment_service.dto.DoctorRequest;
import com.asterlite.appointment_service.dto.DoctorResponse;
import com.asterlite.appointment_service.service.DoctorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService service;

    @PostMapping
    public ResponseEntity<DoctorResponse> create(@Valid @RequestBody DoctorRequest request){
        DoctorResponse created = service.create(request);
        // created(uri) sets status 201 and the Location header. A relative URI still works behind the gateway.
        URI location = URI.create("/api/doctors/" + created.id());
        return ResponseEntity.created(location).body(created);
    }
}
