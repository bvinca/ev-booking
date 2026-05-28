package com.smartcharge.evbooking.web.controller.admin;

import com.smartcharge.evbooking.security.SecurityUser;
import com.smartcharge.evbooking.service.BookingService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/bookings")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBookingController {

    private final BookingService bookingService;

    public AdminBookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("bookings", bookingService.findAll());
        return "admin/bookings/list";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id,
                         @AuthenticationPrincipal SecurityUser principal,
                         RedirectAttributes redirect) {
        bookingService.cancelBooking(id, principal.getId(), true);
        redirect.addFlashAttribute("flashMessage", "Booking #" + id + " cancelled.");
        return "redirect:/admin/bookings";
    }
}
