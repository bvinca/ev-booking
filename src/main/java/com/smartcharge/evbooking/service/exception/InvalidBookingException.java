package com.smartcharge.evbooking.service.exception;

/** Thrown for input that violates booking rules (alignment, ordering, future-only, etc). */
public class InvalidBookingException extends RuntimeException {
    public InvalidBookingException(String message) {
        super(message);
    }
}
