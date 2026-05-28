package com.smartcharge.evbooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the EV charging station booking platform.
 *
 * <p>The application is a layered monolith: controllers (web), services
 * (business logic), repositories (persistence) and domain entities, with
 * cross-cutting security and infrastructure components.</p>
 */
@SpringBootApplication
public class EvBookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(EvBookingApplication.class, args);
    }
}
