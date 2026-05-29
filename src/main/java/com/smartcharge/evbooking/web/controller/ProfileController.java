package com.smartcharge.evbooking.web.controller;

import com.smartcharge.evbooking.domain.User;
import com.smartcharge.evbooking.domain.enums.ConnectorType;
import com.smartcharge.evbooking.security.SecurityUser;
import com.smartcharge.evbooking.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Driver profile: shows account details and lets the driver set their
 * preferred connector type. That preference is auto-applied as the default
 * filter on the station map / list pages.
 */
@Controller
@RequestMapping("/dashboard/profile")
@PreAuthorize("isAuthenticated()")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String profile(@AuthenticationPrincipal SecurityUser principal, Model model) {
        User user = userService.findById(principal.getId());
        model.addAttribute("user", user);
        model.addAttribute("preferredConnector", user.getPreferredConnectorType());
        model.addAttribute("connectorTypes", ConnectorType.values());
        return "dashboard/profile";
    }

    @PostMapping
    public String updatePreferredConnector(
            @RequestParam(name = "preferredConnectorType", required = false) String preferredConnectorType,
            @AuthenticationPrincipal SecurityUser principal,
            RedirectAttributes redirect) {

        ConnectorType type = null;
        if (preferredConnectorType != null && !preferredConnectorType.isBlank()) {
            try {
                type = ConnectorType.valueOf(preferredConnectorType);
            } catch (IllegalArgumentException ex) {
                redirect.addFlashAttribute("flashError", "Unknown connector type.");
                return "redirect:/dashboard/profile";
            }
        }
        userService.updatePreferredConnector(principal.getId(), type);
        redirect.addFlashAttribute("flashSuccess",
            type == null ? "Preference cleared." : "Preferred connector saved.");
        return "redirect:/dashboard/profile";
    }
}
