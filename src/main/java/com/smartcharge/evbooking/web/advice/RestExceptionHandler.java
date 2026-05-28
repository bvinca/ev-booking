package com.smartcharge.evbooking.web.advice;

import com.smartcharge.evbooking.service.exception.BookingConflictException;
import com.smartcharge.evbooking.service.exception.EmailAlreadyUsedException;
import com.smartcharge.evbooking.service.exception.ForbiddenOperationException;
import com.smartcharge.evbooking.service.exception.InvalidBookingException;
import com.smartcharge.evbooking.service.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Translates service-layer exceptions into JSON error responses for
 * controllers under {@code /api/v1}.
 */
@RestControllerAdvice(basePackages = "com.smartcharge.evbooking.web.rest")
public class RestExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(BookingConflictException.class)
    public ResponseEntity<Map<String, Object>> conflict(BookingConflictException ex, HttpServletRequest req) {
        return error(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler({InvalidBookingException.class, EmailAlreadyUsedException.class})
    public ResponseEntity<Map<String, Object>> badRequest(RuntimeException ex, HttpServletRequest req) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<Map<String, Object>> forbidden(ForbiddenOperationException ex, HttpServletRequest req) {
        return error(HttpStatus.FORBIDDEN, ex.getMessage(), req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> denied(AccessDeniedException ex, HttpServletRequest req) {
        return error(HttpStatus.FORBIDDEN, "Access denied.", req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<Map<String, String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .map(this::toMap).toList();
        Map<String, Object> body = baseBody(HttpStatus.BAD_REQUEST, "Validation failed.", req);
        body.put("fieldErrors", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> constraint(ConstraintViolationException ex, HttpServletRequest req) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> generic(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception for {} {}", req.getMethod(), req.getRequestURI(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error.", req);
    }

    private Map<String, String> toMap(FieldError fe) {
        return Map.of(
            "field", fe.getField(),
            "message", fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage()
        );
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus s, String msg, HttpServletRequest req) {
        return ResponseEntity.status(s).body(baseBody(s, msg, req));
    }

    private Map<String, Object> baseBody(HttpStatus s, String msg, HttpServletRequest req) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", s.value());
        body.put("error", s.getReasonPhrase());
        body.put("message", msg);
        body.put("path", req.getRequestURI());
        return body;
    }
}
