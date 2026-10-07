package com.asterlite.patient_service.exception;

// Thrown by the service from PAT-2 onwards. GlobalExceptionHandler turns it into a 404.
public class PatientNotFoundException extends RuntimeException {

    public PatientNotFoundException(Long id){
        // PAT-2 criterion 2 wants the id in the message.
        super("patient with id " + id + " not found");
    }
}
