package com.smartcharge.evbooking.web.controller.admin;

import com.smartcharge.evbooking.domain.Connector;
import com.smartcharge.evbooking.domain.enums.ConnectorType;
import com.smartcharge.evbooking.service.ConnectorService;
import com.smartcharge.evbooking.service.StationService;
import com.smartcharge.evbooking.web.dto.request.ConnectorRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class AdminConnectorController {

    private final StationService stationService;
    private final ConnectorService connectorService;

    public AdminConnectorController(StationService stationService, ConnectorService connectorService) {
        this.stationService = stationService;
        this.connectorService = connectorService;
    }

    @GetMapping("/admin/stations/{stationId}/connectors/new")
    public String newForm(@PathVariable Long stationId, Model model) {
        stationService.findById(stationId); // 404 if missing
        if (!model.containsAttribute("connectorRequest")) {
            model.addAttribute("connectorRequest", new ConnectorRequest());
        }
        model.addAttribute("stationId", stationId);
        model.addAttribute("connectorTypes", ConnectorType.values());
        model.addAttribute("editing", false);
        return "admin/connectors/form";
    }

    @PostMapping("/admin/stations/{stationId}/connectors")
    public String create(@PathVariable Long stationId,
                         @Valid @ModelAttribute("connectorRequest") ConnectorRequest req,
                         BindingResult binding,
                         RedirectAttributes redirect,
                         Model model) {
        if (binding.hasErrors()) {
            model.addAttribute("stationId", stationId);
            model.addAttribute("connectorTypes", ConnectorType.values());
            model.addAttribute("editing", false);
            return "admin/connectors/form";
        }
        stationService.addConnector(stationId, req);
        redirect.addFlashAttribute("flashMessage", "Connector added.");
        return "redirect:/admin/stations";
    }

    @GetMapping("/admin/connectors/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Connector c = connectorService.findById(id);
        ConnectorRequest req = new ConnectorRequest();
        req.setConnectorType(c.getConnectorType());
        req.setPowerKw(c.getPowerKw());
        req.setPricePerHour(c.getPricePerHour());
        req.setActive(c.isActive());
        model.addAttribute("connectorRequest", req);
        model.addAttribute("connectorId", id);
        model.addAttribute("stationId", c.getStation().getId());
        model.addAttribute("connectorTypes", ConnectorType.values());
        model.addAttribute("editing", true);
        return "admin/connectors/form";
    }

    @PostMapping("/admin/connectors/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("connectorRequest") ConnectorRequest req,
                         BindingResult binding,
                         RedirectAttributes redirect,
                         Model model) {
        if (binding.hasErrors()) {
            model.addAttribute("connectorId", id);
            model.addAttribute("connectorTypes", ConnectorType.values());
            model.addAttribute("editing", true);
            return "admin/connectors/form";
        }
        connectorService.update(id, req);
        redirect.addFlashAttribute("flashMessage", "Connector updated.");
        return "redirect:/admin/stations";
    }

    @PostMapping("/admin/connectors/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        connectorService.delete(id);
        redirect.addFlashAttribute("flashMessage", "Connector deleted.");
        return "redirect:/admin/stations";
    }
}
