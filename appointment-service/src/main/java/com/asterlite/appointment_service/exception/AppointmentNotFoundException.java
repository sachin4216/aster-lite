package com.asterlite.appointment_service.exception;

// GlobalExceptionHandler turns it into a 404.
public class AppointmentNotFoundException extends RuntimeException {

    public AppointmentNotFoundException(Long id){
        super("Appointment with id " + id + " not found");
    }
}
