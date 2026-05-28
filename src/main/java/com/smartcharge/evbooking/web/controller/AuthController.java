package com.smartcharge.evbooking.web.controller;

import com.smartcharge.evbooking.service.UserService;
import com.smartcharge.evbooking.service.exception.EmailAlreadyUsedException;
import com.smartcharge.evbooking.web.dto.form.RegisterForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        if (!model.containsAttribute("registerForm")) {
            model.addAttribute("registerForm", new RegisterForm());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerForm") RegisterForm form,
                           BindingResult binding,
                           RedirectAttributes redirect) {

        if (!form.passwordsMatch()) {
            binding.addError(new FieldError("registerForm", "confirmPassword",
                "Passwords do not match."));
        }
        if (binding.hasErrors()) {
            return "auth/register";
        }
        try {
            userService.registerDriver(form.getFullName(), form.getEmail(), form.getPassword());
        } catch (EmailAlreadyUsedException ex) {
            binding.addError(new FieldError("registerForm", "email",
                "This email is already registered."));
            return "auth/register";
        }
        redirect.addFlashAttribute("registrationSuccess", true);
        return "redirect:/login";
    }
}
