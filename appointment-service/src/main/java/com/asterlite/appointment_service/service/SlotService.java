package com.asterlite.appointment_service.service;

import com.asterlite.appointment_service.dto.PageResponse;
import com.asterlite.appointment_service.dto.SlotRequest;
import com.asterlite.appointment_service.dto.SlotResponse;
import com.asterlite.appointment_service.enums.SlotStatus;
import org.springframework.data.domain.Pageable;

public interface SlotService {
    SlotResponse create(Long doctorId, SlotRequest request);
    // status is an optional filter; pass null to skip it.
    PageResponse<SlotResponse> list(Long doctorId, SlotStatus status, Pageable pageable);
}
