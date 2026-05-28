package com.smartcharge.evbooking.service;

import com.smartcharge.evbooking.service.exception.InvalidBookingException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingPolicyTest {

    private final BookingPolicy policy = new BookingPolicy(30, 16, 60);
    private final Instant now = LocalDateTime.of(2026, 6, 1, 10, 0).toInstant(ZoneOffset.UTC);

    private Instant t(int h, int m) {
        return LocalDateTime.of(2026, 6, 1, h, m).toInstant(ZoneOffset.UTC);
    }

    @Test
    void rejects_misaligned_start() {
        assertThatThrownBy(() -> policy.validateRange(t(11, 5), t(11, 35), now))
            .isInstanceOf(InvalidBookingException.class)
            .hasMessageContaining("aligned");
    }

    @Test
    void rejects_misaligned_end() {
        assertThatThrownBy(() -> policy.validateRange(t(11, 0), t(11, 45), now))
            .isInstanceOf(InvalidBookingException.class)
            .hasMessageContaining("aligned");
    }

    @Test
    void rejects_end_before_start() {
        assertThatThrownBy(() -> policy.validateRange(t(12, 0), t(11, 30), now))
            .isInstanceOf(InvalidBookingException.class)
            .hasMessageContaining("End time");
    }

    @Test
    void rejects_equal_start_and_end() {
        assertThatThrownBy(() -> policy.validateRange(t(12, 0), t(12, 0), now))
            .isInstanceOf(InvalidBookingException.class);
    }

    @Test
    void rejects_in_the_past() {
        assertThatThrownBy(() -> policy.validateRange(t(9, 0), t(9, 30), now))
            .isInstanceOf(InvalidBookingException.class)
            .hasMessageContaining("future");
    }

    @Test
    void rejects_beyond_max_advance() {
        Instant far = now.plus(70, ChronoUnit.DAYS);
        assertThatThrownBy(() -> policy.validateRange(far, far.plus(30, ChronoUnit.MINUTES), now))
            .isInstanceOf(InvalidBookingException.class)
            .hasMessageContaining("advance");
    }

    @Test
    void rejects_too_long() {
        Instant start = t(12, 0);
        Instant end   = start.plus(17 * 30, ChronoUnit.MINUTES); // 17 slots → over max
        assertThatThrownBy(() -> policy.validateRange(start, end, now))
            .isInstanceOf(InvalidBookingException.class)
            .hasMessageContaining("slots");
    }

    @Test
    void accepts_single_slot_in_future() {
        policy.validateRange(t(11, 0), t(11, 30), now);  // 30 minutes
    }

    @Test
    void accepts_max_slot_length() {
        Instant start = t(11, 0);
        Instant end   = start.plus(16 * 30, ChronoUnit.MINUTES); // 8 hours
        policy.validateRange(start, end, now);
    }
}
