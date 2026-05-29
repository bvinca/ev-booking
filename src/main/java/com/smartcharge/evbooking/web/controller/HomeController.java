package com.smartcharge.evbooking.web.controller;

import com.smartcharge.evbooking.domain.enums.ConnectorType;
import com.smartcharge.evbooking.domain.enums.RoleName;
import com.smartcharge.evbooking.repository.BookingRepository;
import com.smartcharge.evbooking.repository.ChargingStationRepository;
import com.smartcharge.evbooking.repository.ConnectorRepository;
import com.smartcharge.evbooking.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;

@Controller
public class HomeController {

    private final ChargingStationRepository stationRepository;
    private final ConnectorRepository connectorRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    public HomeController(ChargingStationRepository stationRepository,
                          ConnectorRepository connectorRepository,
                          UserRepository userRepository,
                          BookingRepository bookingRepository) {
        this.stationRepository = stationRepository;
        this.connectorRepository = connectorRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("stationCount", stationRepository.count());
        model.addAttribute("connectorCount", connectorRepository.count());
        model.addAttribute("driverCount", userRepository.countByRole(RoleName.ROLE_DRIVER));
        model.addAttribute("kwhDelivered",
                Math.round(bookingRepository.estimateTotalKwhDelivered()));
        model.addAttribute("connectorTypes", ConnectorType.values());
        model.addAttribute("today", LocalDate.now());
        return "home";
    }
}
