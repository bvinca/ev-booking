package com.smartcharge.evbooking.web.controller;

import com.smartcharge.evbooking.domain.Booking;
import com.smartcharge.evbooking.security.SecurityUser;
import com.smartcharge.evbooking.service.BookingService;
import com.smartcharge.evbooking.service.exception.ForbiddenOperationException;
import com.smartcharge.evbooking.service.exception.InvalidBookingException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Controller
@RequestMapping("/dashboard")
@PreAuthorize("isAuthenticated()")
public class DriverDashboardController {

    private final BookingService bookingService;
    private final Clock clock;

    public DriverDashboardController(BookingService bookingService, Clock clock) {
        this.bookingService = bookingService;
        this.clock = clock;
    }

    @GetMapping
    public String dashboard(@AuthenticationPrincipal SecurityUser principal, Model model) {
        Instant now = Instant.now(clock);
        List<Booking> all = bookingService.findOwnBookings(principal.getId());
        List<Booking> upcoming = all.stream().filter(b -> b.getStartTime().isAfter(now)).toList();
        model.addAttribute("upcoming", upcoming);
        model.addAttribute("totalCount", all.size());
        return "dashboard/driver";
    }

    @GetMapping("/bookings")
    public String myBookings(@AuthenticationPrincipal SecurityUser principal, Model model) {
        Instant now = Instant.now(clock);
        model.addAttribute("bookings", bookingService.findOwnBookings(principal.getId()));
        model.addAttribute("now", now);
        return "dashboard/bookings";
    }

    @PostMapping("/bookings/{id}/cancel")
    public String cancel(@PathVariable Long id,
                         @AuthenticationPrincipal SecurityUser principal,
                         RedirectAttributes redirect) {
        try {
            bookingService.cancelBooking(id, principal.getId(), false);
            redirect.addFlashAttribute("flashSuccess", "Booking cancelled.");
        } catch (ForbiddenOperationException | InvalidBookingException ex) {
            redirect.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/dashboard/bookings";
    }
}
