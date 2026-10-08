package com.asterlite.appointment_service.exception;

// GlobalExceptionHandler turns it into a 404.
public class SlotNotFoundException extends RuntimeException {

    public SlotNotFoundException(Long id){
        super("Slot with id " + id + " not found");
    }
}
