package com.smartcharge.evbooking.web.controller;

import com.smartcharge.evbooking.domain.ChargingStation;
import com.smartcharge.evbooking.domain.enums.ConnectorType;
import com.smartcharge.evbooking.service.StationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/stations")
public class StationViewController {

    private final StationService stationService;

    public StationViewController(StationService stationService) {
        this.stationService = stationService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String city,
                       @RequestParam(required = false) ConnectorType type,
                       @RequestParam(required = false) BigDecimal minKw,
                       Model model) {
        List<ChargingStation> stations = stationService.search(city, type, minKw);
        model.addAttribute("stations", stations);
        model.addAttribute("city", city);
        model.addAttribute("type", type);
        model.addAttribute("minKw", minKw);
        model.addAttribute("connectorTypes", ConnectorType.values());
        return "stations/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        ChargingStation s = stationService.findById(id);
        model.addAttribute("station", s);
        return "stations/detail";
    }
}
