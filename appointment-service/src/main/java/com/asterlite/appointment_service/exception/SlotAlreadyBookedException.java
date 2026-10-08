package com.asterlite.appointment_service.exception;

// GlobalExceptionHandler turns it into a 409.
public class SlotAlreadyBookedException extends RuntimeException {

    public SlotAlreadyBookedException(Long slotId){
        // The message names the slot (APT-7 criterion 3 asks for that too).
        super("Slot with id " + slotId + " is already booked");
    }
}
