package com.example.ticket_booking_system.exception;

public class BookingContentionException extends RuntimeException {
    public BookingContentionException(String message) {
        super(message);
    }
}
