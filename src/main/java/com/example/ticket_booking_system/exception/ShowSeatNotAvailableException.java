package com.example.ticket_booking_system.exception;

public class ShowSeatNotAvailableException extends RuntimeException{
    public ShowSeatNotAvailableException(String msg){
        super(msg);
    }
}
