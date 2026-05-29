package com.smartcharge.evbooking.web.controller;

import com.smartcharge.evbooking.domain.Booking;
import com.smartcharge.evbooking.security.SecurityUser;
import com.smartcharge.evbooking.service.BookingPolicy;
import com.smartcharge.evbooking.service.BookingService;
import com.smartcharge.evbooking.service.exception.BookingConflictException;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Controller
@RequestMapping("/dashboard")
@PreAuthorize("isAuthenticated()")
public class DriverDashboardController {

    private final BookingService bookingService;
    private final BookingPolicy bookingPolicy;
    private final Clock clock;

    public DriverDashboardController(BookingService bookingService, BookingPolicy bookingPolicy, Clock clock) {
        this.bookingService = bookingService;
        this.bookingPolicy = bookingPolicy;
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

    /**
     * Show the modify-booking form pre-filled with the booking's current times.
     * The booking is loaded via the service, which enforces ownership.
     */
    @GetMapping("/bookings/{id}/edit")
    public String editForm(@PathVariable Long id,
                           @AuthenticationPrincipal SecurityUser principal,
                           Model model,
                           RedirectAttributes redirect) {
        Booking booking;
        try {
            booking = bookingService.findOwnedBooking(id, principal.getId(), isAdmin(principal));
        } catch (ForbiddenOperationException ex) {
            redirect.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/dashboard/bookings";
        }
        if (!booking.isInFuture(Instant.now(clock))) {
            redirect.addFlashAttribute("flashError",
                "Bookings can only be modified before the reserved slot begins.");
            return "redirect:/dashboard/bookings";
        }
        model.addAttribute("booking", booking);
        model.addAttribute("slotMinutes", bookingPolicy.getSlotMinutes());
        model.addAttribute("currentStart",
            booking.getStartTime().atOffset(ZoneOffset.UTC).toLocalDateTime().toString());
        model.addAttribute("currentEnd",
            booking.getEndTime().atOffset(ZoneOffset.UTC).toLocalDateTime().toString());
        return "dashboard/booking-edit";
    }

    /**
     * Submit the modified start/end. Connector cannot be changed via modify
     * (assignment requirement: cancel + rebook to switch connector).
     */
    @PostMapping("/bookings/{id}/edit")
    public String editSubmit(@PathVariable Long id,
                             @RequestParam String startTime,
                             @RequestParam String endTime,
                             @AuthenticationPrincipal SecurityUser principal,
                             RedirectAttributes redirect) {
        try {
            Instant start = LocalDateTime.parse(startTime).toInstant(ZoneOffset.UTC);
            Instant end   = LocalDateTime.parse(endTime).toInstant(ZoneOffset.UTC);
            bookingService.modifyBooking(id, principal.getId(), isAdmin(principal), start, end);
            redirect.addFlashAttribute("flashSuccess", "Booking updated.");
            return "redirect:/dashboard/bookings";
        } catch (BookingConflictException | InvalidBookingException | ForbiddenOperationException ex) {
            redirect.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/dashboard/bookings/" + id + "/edit";
        }
    }

    private static boolean isAdmin(SecurityUser principal) {
        return principal.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
