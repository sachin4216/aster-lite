package com.asterlite.patient_service.exception;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email){
        super("A patient with this email " + email + " already exists");
    }
}
