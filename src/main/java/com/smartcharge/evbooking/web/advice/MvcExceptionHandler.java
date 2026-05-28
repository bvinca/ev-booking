package com.smartcharge.evbooking.web.advice;

import com.smartcharge.evbooking.service.exception.ForbiddenOperationException;
import com.smartcharge.evbooking.service.exception.ResourceNotFoundException;
import com.smartcharge.evbooking.web.controller.HomeController;
import com.smartcharge.evbooking.web.controller.StationViewController;
import com.smartcharge.evbooking.web.controller.AuthController;
import com.smartcharge.evbooking.web.controller.BookingViewController;
import com.smartcharge.evbooking.web.controller.DriverDashboardController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Renders Thymeleaf error pages for the MVC controllers (i.e. everything
 * outside the REST API). The REST API has its own JSON-emitting advice.
 */
@ControllerAdvice(assignableTypes = {
    HomeController.class,
    AuthController.class,
    StationViewController.class,
    BookingViewController.class,
    DriverDashboardController.class,
    com.smartcharge.evbooking.web.controller.admin.AdminDashboardController.class,
    com.smartcharge.evbooking.web.controller.admin.AdminStationController.class,
    com.smartcharge.evbooking.web.controller.admin.AdminConnectorController.class,
    com.smartcharge.evbooking.web.controller.admin.AdminBookingController.class,
    com.smartcharge.evbooking.web.controller.admin.AdminUserController.class
})
public class MvcExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(MvcExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(ResourceNotFoundException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "error/404";
    }

    @ExceptionHandler({AccessDeniedException.class, ForbiddenOperationException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String forbidden(Exception ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "error/403";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String generic(Exception ex, Model model) {
        log.error("Unhandled MVC exception", ex);
        model.addAttribute("message", "Something went wrong.");
        return "error/500";
    }
}

/** Plain controller for the /error/403 redirect target referenced in SecurityConfig. */
@Controller
class ErrorPageController {
    @GetMapping("/error/403")
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String forbidden() { return "error/403"; }

    @GetMapping("/error/404")
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound() { return "error/404"; }
}
