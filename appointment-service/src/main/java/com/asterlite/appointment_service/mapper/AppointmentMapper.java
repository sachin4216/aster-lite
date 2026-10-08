package com.asterlite.appointment_service.mapper;

import com.asterlite.appointment_service.dto.AppointmentResponse;
import com.asterlite.appointment_service.entity.Appointment;
import com.asterlite.appointment_service.entity.Slot;
import org.springframework.stereotype.Component;

@Component
public class AppointmentMapper {

    // No toEntity: the request holds only ids, so the service builds the entity itself.
    public AppointmentResponse toResponse(Appointment appointment){
        // slot is LAZY. Reading its times loads it, so this must run inside a transaction.
        Slot slot = appointment.getSlot();
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getPatientId(),
                slot.getId(),
                slot.getDoctor().getId(),
                slot.getStartTime(),
                slot.getEndTime(),
                appointment.getStatus(),
                appointment.getCreatedAt(),
                appointment.getUpdatedAt()
        );
    }
}
