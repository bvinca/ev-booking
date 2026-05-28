package com.smartcharge.evbooking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    /**
     * Inject {@link Clock} rather than calling {@code Instant.now()} directly,
     * so unit tests can substitute a fixed clock and assert deterministic behaviour.
     */
    @Bean
    public Clock systemUtcClock() {
        return Clock.systemUTC();
    }
}
