package com.smartcharge.evbooking.web.controller;

import com.smartcharge.evbooking.domain.Connector;
import com.smartcharge.evbooking.security.SecurityUser;
import com.smartcharge.evbooking.service.BookingPolicy;
import com.smartcharge.evbooking.service.BookingService;
import com.smartcharge.evbooking.service.ConnectorService;
import com.smartcharge.evbooking.service.exception.BookingConflictException;
import com.smartcharge.evbooking.service.exception.InvalidBookingException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

@Controller
@RequestMapping("/bookings")
@PreAuthorize("isAuthenticated()")
public class BookingViewController {

    private final BookingService bookingService;
    private final ConnectorService connectorService;
    private final BookingPolicy policy;

    public BookingViewController(BookingService bookingService,
                                 ConnectorService connectorService,
                                 BookingPolicy policy) {
        this.bookingService = bookingService;
        this.connectorService = connectorService;
        this.policy = policy;
    }

    @GetMapping("/new")
    public String form(@RequestParam Long connectorId,
                       @RequestParam(required = false) String date,
                       Model model) {
        Connector connector = connectorService.findById(connectorId);
        LocalDate day;
        try {
            day = (date != null && !date.isBlank()) ? LocalDate.parse(date) : LocalDate.now(ZoneOffset.UTC);
        } catch (DateTimeParseException ex) {
            day = LocalDate.now(ZoneOffset.UTC);
        }
        model.addAttribute("connector", connector);
        model.addAttribute("station", connector.getStation());
        model.addAttribute("date", day);
        model.addAttribute("slotMinutes", policy.getSlotMinutes());
        model.addAttribute("maxAdvanceDays", policy.getMaxAdvanceDays());
        return "bookings/form";
    }

    @PostMapping
    public String create(@RequestParam Long connectorId,
                         @RequestParam String startTime,
                         @RequestParam String endTime,
                         @AuthenticationPrincipal SecurityUser principal,
                         RedirectAttributes redirect) {
        try {
            Instant start = parseLocalDateTimeAsUtc(startTime);
            Instant end   = parseLocalDateTimeAsUtc(endTime);
            bookingService.createBooking(principal.getId(), connectorId, start, end);
            redirect.addFlashAttribute("flashSuccess", "Booking confirmed.");
            return "redirect:/dashboard/bookings";
        } catch (BookingConflictException | InvalidBookingException ex) {
            redirect.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/bookings/new?connectorId=" + connectorId;
        }
    }

    private static Instant parseLocalDateTimeAsUtc(String s) {
        // Form value is a "datetime-local" like 2026-06-01T10:30
        LocalDateTime ldt = LocalDateTime.parse(s);
        return ldt.toInstant(ZoneOffset.UTC);
    }
}
