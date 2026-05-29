package com.smartcharge.evbooking.web.controller;

import com.smartcharge.evbooking.domain.ChargingStation;
import com.smartcharge.evbooking.domain.User;
import com.smartcharge.evbooking.domain.enums.ConnectorType;
import com.smartcharge.evbooking.security.SecurityUser;
import com.smartcharge.evbooking.service.StationService;
import com.smartcharge.evbooking.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/stations")
public class StationViewController {

    private final StationService stationService;
    private final UserService userService;

    public StationViewController(StationService stationService, UserService userService) {
        this.stationService = stationService;
        this.userService = userService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String city,
                       @RequestParam(required = false) ConnectorType type,
                       @RequestParam(required = false) BigDecimal minKw,
                       @AuthenticationPrincipal SecurityUser principal,
                       WebRequest request,
                       Model model) {

        // If the authenticated driver hasn't passed an explicit type filter
        // (the param is completely absent from the query — different from
        // "type=" which means "user explicitly chose Any"), apply their saved
        // preference as the default.
        ConnectorType appliedType = type;
        boolean defaultedFromProfile = false;
        if (appliedType == null
            && principal != null
            && request.getParameter("type") == null) {
            User me = userService.findById(principal.getId());
            if (me.getPreferredConnectorType() != null) {
                appliedType = me.getPreferredConnectorType();
                defaultedFromProfile = true;
            }
        }

        List<ChargingStation> stations = stationService.search(city, appliedType, minKw);
        model.addAttribute("stations", stations);
        model.addAttribute("city", city);
        model.addAttribute("type", appliedType);
        model.addAttribute("minKw", minKw);
        model.addAttribute("connectorTypes", ConnectorType.values());
        model.addAttribute("defaultedFromProfile", defaultedFromProfile);
        return "stations/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        ChargingStation s = stationService.findById(id);
        model.addAttribute("station", s);
        return "stations/detail";
    }
}
