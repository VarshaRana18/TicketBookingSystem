package com.example.ticket_booking_system.exception;

public class DuplicateEmailsException extends RuntimeException{
    public DuplicateEmailsException(String msg){
        super(msg);
    }
}
