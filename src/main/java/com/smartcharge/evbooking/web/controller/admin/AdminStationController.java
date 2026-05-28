package com.smartcharge.evbooking.web.controller.admin;

import com.smartcharge.evbooking.domain.ChargingStation;
import com.smartcharge.evbooking.service.StationService;
import com.smartcharge.evbooking.web.dto.request.StationRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/stations")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStationController {

    private final StationService stationService;

    public AdminStationController(StationService stationService) {
        this.stationService = stationService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("stations", stationService.findAll());
        return "admin/stations/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        if (!model.containsAttribute("stationRequest")) {
            model.addAttribute("stationRequest", new StationRequest());
        }
        model.addAttribute("editing", false);
        return "admin/stations/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("stationRequest") StationRequest req,
                         BindingResult binding,
                         RedirectAttributes redirect,
                         Model model) {
        if (binding.hasErrors()) {
            model.addAttribute("editing", false);
            return "admin/stations/form";
        }
        ChargingStation s = stationService.create(req);
        redirect.addFlashAttribute("flashMessage", "Station '" + s.getName() + "' created.");
        return "redirect:/admin/stations";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        ChargingStation s = stationService.findById(id);
        StationRequest req = new StationRequest();
        req.setName(s.getName());
        req.setAddress(s.getAddress());
        req.setCity(s.getCity());
        req.setLatitude(s.getLatitude());
        req.setLongitude(s.getLongitude());
        req.setDescription(s.getDescription());
        model.addAttribute("stationRequest", req);
        model.addAttribute("stationId", id);
        model.addAttribute("editing", true);
        return "admin/stations/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("stationRequest") StationRequest req,
                         BindingResult binding,
                         RedirectAttributes redirect,
                         Model model) {
        if (binding.hasErrors()) {
            model.addAttribute("editing", true);
            model.addAttribute("stationId", id);
            return "admin/stations/form";
        }
        stationService.update(id, req);
        redirect.addFlashAttribute("flashMessage", "Station updated.");
        return "redirect:/admin/stations";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        stationService.delete(id);
        redirect.addFlashAttribute("flashMessage", "Station deleted.");
        return "redirect:/admin/stations";
    }
}
