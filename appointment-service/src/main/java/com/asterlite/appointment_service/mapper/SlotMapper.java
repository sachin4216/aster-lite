package com.asterlite.appointment_service.mapper;

import com.asterlite.appointment_service.dto.SlotRequest;
import com.asterlite.appointment_service.dto.SlotResponse;
import com.asterlite.appointment_service.entity.Doctor;
import com.asterlite.appointment_service.entity.Slot;
import org.springframework.stereotype.Component;

@Component
public class SlotMapper {

    // The doctor is passed in separately: it comes from the URL, not from the request body.
    public Slot toEntity(SlotRequest request, Doctor doctor){
        Slot slot = new Slot();
        slot.setDoctor(doctor);
        slot.setStartTime(request.startTime());
        slot.setEndTime(request.endTime());
        return slot;
    }

    public SlotResponse toResponse(Slot slot){
        return new SlotResponse(
                slot.getId(),
                // Reading only the id of a LAZY doctor runs no SELECT: the id is already in the slot row.
                slot.getDoctor().getId(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getStatus()
        );
    }
}
