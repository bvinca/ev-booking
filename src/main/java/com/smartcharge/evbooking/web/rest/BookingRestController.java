package com.smartcharge.evbooking.web.rest;

import com.smartcharge.evbooking.domain.Booking;
import com.smartcharge.evbooking.security.SecurityUser;
import com.smartcharge.evbooking.service.AvailabilityService;
import com.smartcharge.evbooking.service.BookingService;
import com.smartcharge.evbooking.web.dto.request.BookingModifyRequest;
import com.smartcharge.evbooking.web.dto.request.BookingRequest;
import com.smartcharge.evbooking.web.dto.response.BookingDto;
import com.smartcharge.evbooking.web.mapper.BookingMapper;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
public class BookingRestController {

    private final BookingService bookingService;
    private final AvailabilityService availabilityService;
    private final BookingMapper mapper;

    public BookingRestController(BookingService bookingService,
                                 AvailabilityService availabilityService,
                                 BookingMapper mapper) {
        this.bookingService = bookingService;
        this.availabilityService = availabilityService;
        this.mapper = mapper;
    }

    // ----- Availability (public) -----

    @GetMapping("/api/v1/connectors/{id}/availability")
    public List<AvailabilityService.Slot> availability(
        @PathVariable("id") Long connectorId,
        @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return availabilityService.getSlotsForDay(connectorId, date);
    }

    // ----- Driver: own bookings -----

    @PostMapping("/api/v1/bookings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BookingDto> create(@Valid @RequestBody BookingRequest req,
                                             @AuthenticationPrincipal SecurityUser principal) {
        Booking b = bookingService.createBooking(
            principal.getId(), req.getConnectorId(), req.getStartTime(), req.getEndTime());
        return ResponseEntity
            .created(URI.create("/api/v1/bookings/" + b.getId()))
            .body(mapper.toDto(b));
    }

    @GetMapping("/api/v1/bookings/me")
    @PreAuthorize("isAuthenticated()")
    public List<BookingDto> myBookings(@AuthenticationPrincipal SecurityUser principal) {
        return bookingService.findOwnBookings(principal.getId()).stream().map(mapper::toDto).toList();
    }

    @PatchMapping("/api/v1/bookings/{id}")
    @PreAuthorize("isAuthenticated()")
    public BookingDto modify(@PathVariable Long id,
                             @Valid @RequestBody BookingModifyRequest req,
                             @AuthenticationPrincipal SecurityUser principal) {
        boolean admin = principal.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        Booking b = bookingService.modifyBooking(
            id, principal.getId(), admin, req.getStartTime(), req.getEndTime());
        return mapper.toDto(b);
    }

    @DeleteMapping("/api/v1/bookings/{id}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long id,
                       @AuthenticationPrincipal SecurityUser principal) {
        boolean admin = principal.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        bookingService.cancelBooking(id, principal.getId(), admin);
    }

    // ----- Admin: all bookings -----

    @GetMapping("/api/v1/bookings")
    @PreAuthorize("hasRole('ADMIN')")
    public List<BookingDto> all() {
        return bookingService.findAll().stream().map(mapper::toDto).toList();
    }
}
