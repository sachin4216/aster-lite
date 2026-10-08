package com.asterlite.appointment_service.exception;

// A rule that compares two fields, so @Valid cannot catch it. GlobalExceptionHandler turns it into a 400.
public class InvalidSlotTimeException extends RuntimeException {

    public InvalidSlotTimeException(String message){
        super(message);
    }
}
