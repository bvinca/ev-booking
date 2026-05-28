package com.smartcharge.evbooking.service.exception;

/** Thrown when a booking cannot be created or modified because of an overlap. */
public class BookingConflictException extends RuntimeException {
    public BookingConflictException(String message) {
        super(message);
    }
}
