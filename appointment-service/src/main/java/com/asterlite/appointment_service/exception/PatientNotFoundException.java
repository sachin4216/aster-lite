package com.asterlite.appointment_service.exception;

// Thrown by PatientClient when patient-service answers 404. GlobalExceptionHandler turns it into a 404.
public class PatientNotFoundException extends RuntimeException {

    public PatientNotFoundException(Long id){
        // APT-5 criterion 3 wants the patient id in the message.
        super("Patient with id " + id + " not found");
    }
}
