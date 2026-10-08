package com.asterlite.appointment_service.controller;

import com.asterlite.appointment_service.dto.AppointmentRequest;
import com.asterlite.appointment_service.dto.AppointmentResponse;
import com.asterlite.appointment_service.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService service;

    @PostMapping
    public ResponseEntity<AppointmentResponse> book(@Valid @RequestBody AppointmentRequest request){
        AppointmentResponse created = service.book(request);
        // created(uri) sets status 201 and the Location header. A relative URI still works behind the gateway.
        URI location = URI.create("/api/appointments/" + created.id());
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponse> getById(@PathVariable Long id){
        return ResponseEntity.ok(service.getById(id));
    }
}
