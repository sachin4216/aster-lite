package com.asterlite.appointment_service.exception;

// GlobalExceptionHandler turns it into a 404.
public class DoctorNotFoundException extends RuntimeException {

    public DoctorNotFoundException(Long id){
        // APT-3 criterion 2 wants the id in the message.
        super("Doctor with id " + id + " not found");
    }
}
