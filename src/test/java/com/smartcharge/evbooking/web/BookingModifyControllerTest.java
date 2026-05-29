package com.smartcharge.evbooking.web;

import com.smartcharge.evbooking.AbstractPostgresIT;
import com.smartcharge.evbooking.domain.Booking;
import com.smartcharge.evbooking.domain.ChargingStation;
import com.smartcharge.evbooking.domain.Connector;
import com.smartcharge.evbooking.domain.User;
import com.smartcharge.evbooking.domain.enums.BookingStatus;
import com.smartcharge.evbooking.domain.enums.ConnectorType;
import com.smartcharge.evbooking.repository.BookingRepository;
import com.smartcharge.evbooking.repository.ChargingStationRepository;
import com.smartcharge.evbooking.service.BookingService;
import com.smartcharge.evbooking.service.UserService;
import com.smartcharge.evbooking.service.exception.EmailAlreadyUsedException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end web flow for modifying a booking:
 * GET edit form → POST new times → service updates the row → redirect to bookings list.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(initializers = AbstractPostgresIT.Initializer.class)
class BookingModifyControllerTest extends AbstractPostgresIT {

    static final String DRIVER_EMAIL = "modify-test-driver@example.com";

    @Autowired MockMvc mvc;
    @Autowired UserService userService;
    @Autowired BookingService bookingService;
    @Autowired BookingRepository bookingRepository;
    @Autowired ChargingStationRepository stationRepository;

    private Long bookingId;

    @BeforeAll
    void setUp() {
        User driver;
        try {
            driver = userService.registerDriver("Modify Tester", DRIVER_EMAIL, "password123");
        } catch (EmailAlreadyUsedException ignored) {
            driver = userService.findByEmail(DRIVER_EMAIL);
        }

        ChargingStation s = new ChargingStation(
            "Modify Station " + System.nanoTime(),
            "Plateia 1", "Thessaloniki",
            new BigDecimal("40.6320"), new BigDecimal("22.9417"), "");
        Connector c = new Connector(s, ConnectorType.CCS,
            new BigDecimal("50.0"), new BigDecimal("0.30"));
        s.getConnectors().add(c);
        stationRepository.save(s);
        Long connectorId = s.getConnectors().get(0).getId();

        Instant start = tomorrowAt(10);
        Instant end   = tomorrowAt(11);
        Booking b = bookingService.createBooking(driver.getId(), connectorId, start, end);
        this.bookingId = b.getId();
    }

    @Test
    @WithUserDetails(DRIVER_EMAIL)
    void edit_form_renders_with_current_values() throws Exception {
        mvc.perform(get("/dashboard/bookings/" + bookingId + "/edit"))
            .andExpect(status().isOk())
            .andExpect(view().name("dashboard/booking-edit"))
            .andExpect(model().attributeExists("booking", "currentStart", "currentEnd", "slotMinutes"));
    }

    @Test
    @WithUserDetails(DRIVER_EMAIL)
    void posting_new_times_updates_the_booking() throws Exception {
        Instant newStart = tomorrowAt(14);
        Instant newEnd   = tomorrowAt(15);
        String startStr = LocalDateTime.ofInstant(newStart, ZoneOffset.UTC).toString();
        String endStr   = LocalDateTime.ofInstant(newEnd,   ZoneOffset.UTC).toString();

        mvc.perform(post("/dashboard/bookings/" + bookingId + "/edit")
                .with(csrf())
                .param("startTime", startStr)
                .param("endTime",   endStr))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/dashboard/bookings"));

        Booking after = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(after.getStartTime()).isEqualTo(newStart);
        assertThat(after.getEndTime()).isEqualTo(newEnd);
        assertThat(after.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    private static Instant tomorrowAt(int hourUtc) {
        return LocalDateTime.now(ZoneOffset.UTC).plusDays(1)
            .withHour(hourUtc).withMinute(0).withSecond(0).withNano(0)
            .toInstant(ZoneOffset.UTC);
    }
}
