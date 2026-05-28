package com.smartcharge.evbooking.web.controller.admin;

import com.smartcharge.evbooking.repository.BookingRepository;
import com.smartcharge.evbooking.repository.ChargingStationRepository;
import com.smartcharge.evbooking.repository.ConnectorRepository;
import com.smartcharge.evbooking.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final ChargingStationRepository stationRepository;
    private final ConnectorRepository connectorRepository;
    private final BookingRepository bookingRepository;

    public AdminDashboardController(UserRepository userRepository,
                                    ChargingStationRepository stationRepository,
                                    ConnectorRepository connectorRepository,
                                    BookingRepository bookingRepository) {
        this.userRepository = userRepository;
        this.stationRepository = stationRepository;
        this.connectorRepository = connectorRepository;
        this.bookingRepository = bookingRepository;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("userCount", userRepository.count());
        model.addAttribute("stationCount", stationRepository.count());
        model.addAttribute("connectorCount", connectorRepository.count());
        model.addAttribute("bookingCount", bookingRepository.count());
        return "admin/dashboard";
    }
}
