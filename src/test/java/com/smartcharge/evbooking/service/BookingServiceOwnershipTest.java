package com.smartcharge.evbooking.service;

import com.smartcharge.evbooking.AbstractPostgresIT;
import com.smartcharge.evbooking.domain.Booking;
import com.smartcharge.evbooking.domain.ChargingStation;
import com.smartcharge.evbooking.domain.Connector;
import com.smartcharge.evbooking.domain.User;
import com.smartcharge.evbooking.domain.enums.BookingStatus;
import com.smartcharge.evbooking.domain.enums.ConnectorType;
import com.smartcharge.evbooking.repository.BookingRepository;
import com.smartcharge.evbooking.repository.ChargingStationRepository;
import com.smartcharge.evbooking.service.exception.ForbiddenOperationException;
import com.smartcharge.evbooking.service.exception.InvalidBookingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies ownership rules: drivers can only modify/cancel their own
 * bookings, only future bookings, and only while CONFIRMED.
 */
@SpringBootTest
@ContextConfiguration(initializers = AbstractPostgresIT.Initializer.class)
class BookingServiceOwnershipTest extends AbstractPostgresIT {

    @Autowired BookingService bookingService;
    @Autowired UserService userService;
    @Autowired ChargingStationRepository stationRepository;
    @Autowired BookingRepository bookingRepository;

    private Long connectorId;
    private Long aliceId;
    private Long bobId;

    @BeforeEach
    void setUp() {
        ChargingStation s = new ChargingStation(
            "Ownership Station " + System.nanoTime(),
            "1 Lane", "City",
            new BigDecimal("53.0"), new BigDecimal("-1.0"), "");
        Connector c = new Connector(s, ConnectorType.CCS, new BigDecimal("50.0"), new BigDecimal("5.00"));
        s.getConnectors().add(c);
        stationRepository.save(s);
        connectorId = s.getConnectors().get(0).getId();

        long n = System.nanoTime();
        aliceId = userService.registerDriver("Alice", "alice" + n + "@example.com", "password123").getId();
        bobId   = userService.registerDriver("Bob",   "bob"   + n + "@example.com", "password123").getId();
    }

    private Instant tomorrow(int hour) {
        return LocalDateTime.now(ZoneOffset.UTC).plusDays(1)
            .withHour(hour).withMinute(0).withSecond(0).withNano(0)
            .toInstant(ZoneOffset.UTC);
    }

    @Test
    void driver_cannot_cancel_other_drivers_booking() {
        Booking aliceBooking = bookingService.createBooking(
            aliceId, connectorId, tomorrow(10), tomorrow(11));

        assertThatThrownBy(() ->
            bookingService.cancelBooking(aliceBooking.getId(), bobId, false))
            .isInstanceOf(ForbiddenOperationException.class);

        // Booking should still be CONFIRMED
        Booking after = bookingRepository.findById(aliceBooking.getId()).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void admin_can_cancel_any_booking() {
        Booking aliceBooking = bookingService.createBooking(
            aliceId, connectorId, tomorrow(12), tomorrow(13));

        bookingService.cancelBooking(aliceBooking.getId(), bobId, true);  // bob acting as admin

        Booking after = bookingRepository.findById(aliceBooking.getId()).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void driver_cannot_modify_other_drivers_booking() {
        Booking b = bookingService.createBooking(aliceId, connectorId, tomorrow(14), tomorrow(15));
        assertThatThrownBy(() ->
            bookingService.modifyBooking(b.getId(), bobId, false, tomorrow(15), tomorrow(16)))
            .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    @Transactional
    void cannot_modify_past_booking() {
        // Manually plant a booking in the past (bypasses service validation)
        Connector c = stationRepository.findAll().stream()
            .filter(s -> s.getConnectors().stream().anyMatch(x -> x.getId().equals(connectorId)))
            .findFirst().orElseThrow().getConnectors().get(0);
        User alice = userService.findById(aliceId);
        Booking past = new Booking(alice, c,
            Instant.now().minus(2, ChronoUnit.HOURS),
            Instant.now().minus(1, ChronoUnit.HOURS));
        past = bookingRepository.saveAndFlush(past);

        Long id = past.getId();
        assertThatThrownBy(() ->
            bookingService.modifyBooking(id, aliceId, false, tomorrow(16), tomorrow(17)))
            .isInstanceOf(InvalidBookingException.class)
            .hasMessageContaining("before the reserved slot begins");
    }
}
