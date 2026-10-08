package com.asterlite.appointment_service.exception;

// The patient exists but is deactivated. GlobalExceptionHandler turns it into a 409.
public class PatientInactiveException extends RuntimeException {

    public PatientInactiveException(Long id){
        super("Patient with id " + id + " is inactive and cannot book an appointment");
    }
}
