package com.smartcharge.evbooking.service;

import com.smartcharge.evbooking.AbstractPostgresIT;
import com.smartcharge.evbooking.domain.ChargingStation;
import com.smartcharge.evbooking.domain.Connector;
import com.smartcharge.evbooking.domain.User;
import com.smartcharge.evbooking.domain.enums.BookingStatus;
import com.smartcharge.evbooking.domain.enums.ConnectorType;
import com.smartcharge.evbooking.repository.BookingRepository;
import com.smartcharge.evbooking.repository.ChargingStationRepository;
import com.smartcharge.evbooking.service.exception.BookingConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The headline test for the booking system.
 *
 * <p>Spawns {@code N} threads that all try to book the same connector for the
 * same 30-minute slot at the same moment. The system must accept exactly one
 * booking and reject the others with a {@link BookingConflictException}.</p>
 */
@SpringBootTest
@ContextConfiguration(initializers = AbstractPostgresIT.Initializer.class)
class BookingServiceConcurrencyTest extends AbstractPostgresIT {

    @Autowired private BookingService bookingService;
    @Autowired private UserService userService;
    @Autowired private ChargingStationRepository stationRepository;
    @Autowired private BookingRepository bookingRepository;

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NEVER)
    void only_one_concurrent_booking_wins_for_same_slot() throws Exception {
        // Arrange — fresh station + connector + N drivers
        Long connectorId = seedStationAndGetConnectorId();
        int N = 20;
        List<Long> driverIds = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            driverIds.add(userService.registerDriver(
                "Racer " + i, "racer" + i + "@example.com", "password123").getId());
        }
        // All threads target the same slot, far enough in the future to pass policy.
        Instant start = LocalDateTime.now(ZoneOffset.UTC)
            .plusDays(1).withMinute(0).withSecond(0).withNano(0).toInstant(ZoneOffset.UTC);
        Instant end = start.plusSeconds(30 * 60);

        // Act — all threads start at the same moment via a CountDownLatch
        ExecutorService pool = Executors.newFixedThreadPool(N);
        CountDownLatch gate = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();
        List<Future<Throwable>> futures = new ArrayList<>();
        for (Long driverId : driverIds) {
            futures.add(pool.submit(() -> {
                try {
                    gate.await();
                    bookingService.createBooking(driverId, connectorId, start, end);
                    successes.incrementAndGet();
                    return null;
                } catch (BookingConflictException e) {
                    conflicts.incrementAndGet();
                    return e;
                } catch (Throwable t) {
                    return t;
                }
            }));
        }
        gate.countDown();
        pool.shutdown();
        boolean finished = pool.awaitTermination(30, TimeUnit.SECONDS);
        assertThat(finished).as("workers finished in time").isTrue();

        // Assert — exactly one winner, all others conflict, no other exceptions
        long otherErrors = futures.stream().map(f -> {
            try { return f.get(); } catch (Exception e) { return e; }
        }).filter(t -> t != null && !(t instanceof BookingConflictException)).count();

        assertThat(successes.get())
            .as("Exactly one booking should succeed for a contested slot")
            .isEqualTo(1);
        assertThat(conflicts.get())
            .as("Every other thread should observe a BookingConflictException")
            .isEqualTo(N - 1);
        assertThat(otherErrors)
            .as("No exceptions other than BookingConflictException should occur")
            .isEqualTo(0);

        // And the database should show exactly one CONFIRMED booking on that connector.
        long confirmed = bookingRepository.findAll().stream()
            .filter(b -> b.getConnector().getId().equals(connectorId))
            .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
            .count();
        assertThat(confirmed).isEqualTo(1);
    }

    private Long seedStationAndGetConnectorId() {
        ChargingStation s = new ChargingStation(
            "Test Station " + System.nanoTime(),
            "1 Test Lane", "Testville",
            new BigDecimal("53.0"), new BigDecimal("-1.0"),
            "Concurrency test station");
        Connector c = new Connector(s, ConnectorType.CCS,
            new BigDecimal("50.0"), new BigDecimal("5.00"));
        s.getConnectors().add(c);
        s = stationRepository.save(s);
        return s.getConnectors().get(0).getId();
    }
}
