package com.asterlite.appointment_service.controller;

import com.asterlite.appointment_service.dto.AppointmentRequest;
import com.asterlite.appointment_service.dto.AppointmentResponse;
import com.asterlite.appointment_service.enums.AppointmentStatus;
import com.asterlite.appointment_service.exception.SlotAlreadyBookedException;
import com.asterlite.appointment_service.service.AppointmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Loads only the web layer: this controller, GlobalExceptionHandler and the filter. No database.
@WebMvcTest(AppointmentController.class)
class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Replaces the real AppointmentService bean with a Mockito mock.
    @MockitoBean
    private AppointmentService appointmentService;

    @Test
    void book_validRequest_returns201WithLocation() throws Exception {
        Instant start = Instant.parse("2026-10-12T09:00:00Z");
        Instant now = Instant.parse("2026-10-09T05:10:00Z");
        when(appointmentService.book(new AppointmentRequest(1L, 7L)))
                .thenReturn(new AppointmentResponse(3L, 1L, 7L, 2L, start, start.plusSeconds(1800),
                        AppointmentStatus.PENDING, now, now));

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "patientId": 1, "slotId": 7 }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/appointments/3"))
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.startTime").value("2026-10-12T09:00:00Z"));
    }

    @Test
    void book_withoutSlotId_returns400WithOneError() throws Exception {
        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "patientId": 1 }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("slotId"));

        // Validation failed before the controller method ran.
        verifyNoInteractions(appointmentService);
    }

    @Test
    void book_conflictFromService_returns409AsApiErrorResponse() throws Exception {
        when(appointmentService.book(any())).thenThrow(new SlotAlreadyBookedException(7L));

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "patientId": 1, "slotId": 7 }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Slot with id 7 is already booked"))
                .andExpect(jsonPath("$.path").value("/api/appointments"))
                .andExpect(jsonPath("$.errors").isEmpty());
    }
}
