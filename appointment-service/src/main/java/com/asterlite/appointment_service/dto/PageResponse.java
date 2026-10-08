package com.asterlite.appointment_service.dto;

import org.springframework.data.domain.Page;

import java.util.List;

// Your own page shape, the same as in patient-service: "data", not Spring's "content".
public record PageResponse<T>(
        List<T> data,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    // Copies only the five fields the API promises.
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
