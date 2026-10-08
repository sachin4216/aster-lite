package com.asterlite.appointment_service.controller;

import com.asterlite.appointment_service.service.SlotService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Loads only the web layer: this controller, GlobalExceptionHandler and the filter. No database.
@WebMvcTest(SlotController.class)
class SlotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Replaces the real SlotService bean with a Mockito mock.
    @MockitoBean
    private SlotService slotService;

    @Test
    void list_sizeNotANumber_returns400() throws Exception {
        mockMvc.perform(get("/api/doctors/1/slots").param("size", "abc"))
                .andExpect(status().isBadRequest())
                // The body is ApiErrorResponse, built by handleTypeMismatch.
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid value 'abc' for parameter 'size'"))
                .andExpect(jsonPath("$.path").value("/api/doctors/1/slots"));

        // The request was rejected before it reached the service.
        verifyNoInteractions(slotService);
    }
}
