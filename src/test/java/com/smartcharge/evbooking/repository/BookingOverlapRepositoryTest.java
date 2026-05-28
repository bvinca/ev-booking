package com.smartcharge.evbooking.repository;

import com.smartcharge.evbooking.AbstractPostgresIT;
import com.smartcharge.evbooking.domain.Booking;
import com.smartcharge.evbooking.domain.ChargingStation;
import com.smartcharge.evbooking.domain.Connector;
import com.smartcharge.evbooking.domain.Role;
import com.smartcharge.evbooking.domain.User;
import com.smartcharge.evbooking.domain.enums.BookingStatus;
import com.smartcharge.evbooking.domain.enums.ConnectorType;
import com.smartcharge.evbooking.domain.enums.RoleName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = AbstractPostgresIT.Initializer.class)
class BookingOverlapRepositoryTest extends AbstractPostgresIT {

    @Autowired BookingRepository bookings;
    @Autowired ConnectorRepository connectors;
    @Autowired ChargingStationRepository stations;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;

    private Connector connector;
    private User user;

    @BeforeEach
    void setUp() {
        ChargingStation s = new ChargingStation(
            "Repo Station " + System.nanoTime(),
            "1 Lane", "City",
            new BigDecimal("53.0"), new BigDecimal("-1.0"), "");
        Connector c = new Connector(s, ConnectorType.CCS, new BigDecimal("50.0"), new BigDecimal("5.00"));
        s.getConnectors().add(c);
        stations.save(s);
        this.connector = s.getConnectors().get(0);

        Role driverRole = roles.findByName(RoleName.ROLE_DRIVER).orElseThrow();
        User u = new User("Repo Tester " + System.nanoTime(),
            "repo" + System.nanoTime() + "@example.com", "x");
        u.addRole(driverRole);
        this.user = users.save(u);
    }

    private Instant t(int day, int h, int m) {
        return LocalDateTime.of(2030, 1, day, h, m).toInstant(ZoneOffset.UTC);
    }

    private Booking confirmed(Instant s, Instant e) {
        Booking b = new Booking(user, connector, s, e);
        b.setStatus(BookingStatus.CONFIRMED);
        return bookings.save(b);
    }

    @Test
    void detects_exact_match() {
        confirmed(t(1, 10, 0), t(1, 10, 30));
        List<Booking> hits = bookings.findOverlapping(connector.getId(), t(1, 10, 0), t(1, 10, 30), null);
        assertThat(hits).hasSize(1);
    }

    @Test
    void detects_contained_window() {
        confirmed(t(1, 10, 0), t(1, 11, 0));
        List<Booking> hits = bookings.findOverlapping(connector.getId(), t(1, 10, 15), t(1, 10, 45), null);
        assertThat(hits).hasSize(1);
    }

    @Test
    void detects_straddling_start() {
        confirmed(t(1, 10, 0), t(1, 11, 0));
        List<Booking> hits = bookings.findOverlapping(connector.getId(), t(1, 9, 30), t(1, 10, 30), null);
        assertThat(hits).hasSize(1);
    }

    @Test
    void detects_straddling_end() {
        confirmed(t(1, 10, 0), t(1, 11, 0));
        List<Booking> hits = bookings.findOverlapping(connector.getId(), t(1, 10, 30), t(1, 11, 30), null);
        assertThat(hits).hasSize(1);
    }

    @Test
    void allows_back_to_back_no_overlap() {
        confirmed(t(1, 10, 0), t(1, 10, 30));
        List<Booking> hits = bookings.findOverlapping(connector.getId(), t(1, 10, 30), t(1, 11, 0), null);
        assertThat(hits)
            .as("[10:00,10:30) and [10:30,11:00) must NOT count as overlapping")
            .isEmpty();
    }

    @Test
    void excludes_self_in_modify_scenario() {
        Booking b = confirmed(t(1, 10, 0), t(1, 11, 0));
        List<Booking> hits = bookings.findOverlapping(connector.getId(), t(1, 10, 0), t(1, 11, 0), b.getId());
        assertThat(hits)
            .as("when modifying, the row being changed should be excluded from overlap detection")
            .isEmpty();
    }

    @Test
    void ignores_cancelled_bookings() {
        Booking b = confirmed(t(1, 10, 0), t(1, 11, 0));
        b.setStatus(BookingStatus.CANCELLED);
        bookings.save(b);
        List<Booking> hits = bookings.findOverlapping(connector.getId(), t(1, 10, 0), t(1, 11, 0), null);
        assertThat(hits).isEmpty();
    }

    @Test
    void database_exclude_constraint_rejects_direct_overlap_insert() {
        confirmed(t(1, 10, 0), t(1, 11, 0));
        Booking second = new Booking(user, connector, t(1, 10, 30), t(1, 11, 30));
        second.setStatus(BookingStatus.CONFIRMED);
        assertThatThrownBy(() -> {
            bookings.saveAndFlush(second);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
