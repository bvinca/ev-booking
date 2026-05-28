package com.smartcharge.evbooking.service.exception;

/** Thrown when an authenticated user attempts an action they are not permitted to take. */
public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}
