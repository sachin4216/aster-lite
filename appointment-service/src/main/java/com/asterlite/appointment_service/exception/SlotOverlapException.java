package com.asterlite.appointment_service.exception;

// GlobalExceptionHandler turns it into a 409.
public class SlotOverlapException extends RuntimeException {

    public SlotOverlapException(Long doctorId){
        super("Doctor with id " + doctorId + " already has a slot that overlaps this time range");
    }
}
