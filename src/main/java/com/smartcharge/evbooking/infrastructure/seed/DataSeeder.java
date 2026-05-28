package com.smartcharge.evbooking.infrastructure.seed;

import com.smartcharge.evbooking.domain.ChargingStation;
import com.smartcharge.evbooking.domain.Connector;
import com.smartcharge.evbooking.domain.enums.ConnectorType;
import com.smartcharge.evbooking.repository.ChargingStationRepository;
import com.smartcharge.evbooking.service.UserService;
import com.smartcharge.evbooking.service.exception.EmailAlreadyUsedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Loads a small set of demo stations / connectors / users into an empty DB
 * for local development. Activated only when {@code app.seed.enabled=true}
 * (the {@code dev} profile sets this).
 */
@Component
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final ChargingStationRepository stationRepository;
    private final UserService userService;
    private final boolean enabled;

    public DataSeeder(ChargingStationRepository stationRepository,
                      UserService userService,
                      @Value("${app.seed.enabled:false}") boolean enabled) {
        this.stationRepository = stationRepository;
        this.userService = userService;
        this.enabled = enabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        if (!enabled) return;
        if (stationRepository.count() > 0) {
            log.debug("DB not empty; skipping seed.");
            return;
        }
        log.info("Seeding demo data...");

        try {
            userService.registerDriver("Demo Driver", "driver@example.com", "password123");
        } catch (EmailAlreadyUsedException ignored) { }

        seedStation("York Central", "Coppergate Walk", "York",
            new BigDecimal("53.957800"), new BigDecimal("-1.080500"),
            new Connector(null, ConnectorType.CCS,    new BigDecimal("50.0"),  new BigDecimal("6.50")),
            new Connector(null, ConnectorType.TYPE2,  new BigDecimal("22.0"),  new BigDecimal("3.20")),
            new Connector(null, ConnectorType.CHADEMO,new BigDecimal("50.0"),  new BigDecimal("6.50"))
        );

        seedStation("Leeds Trinity", "Albion St", "Leeds",
            new BigDecimal("53.798200"), new BigDecimal("-1.543800"),
            new Connector(null, ConnectorType.CCS,    new BigDecimal("150.0"), new BigDecimal("11.00")),
            new Connector(null, ConnectorType.TYPE2,  new BigDecimal("22.0"),  new BigDecimal("3.50"))
        );

        seedStation("Manchester Piccadilly", "London Rd", "Manchester",
            new BigDecimal("53.477500"), new BigDecimal("-2.230900"),
            new Connector(null, ConnectorType.CCS,    new BigDecimal("350.0"), new BigDecimal("18.50")),
            new Connector(null, ConnectorType.TESLA,  new BigDecimal("250.0"), new BigDecimal("15.00")),
            new Connector(null, ConnectorType.TYPE2,  new BigDecimal("22.0"),  new BigDecimal("3.20"))
        );

        seedStation("London King's Cross", "York Way", "London",
            new BigDecimal("51.530900"), new BigDecimal("-0.123700"),
            new Connector(null, ConnectorType.CCS,    new BigDecimal("150.0"), new BigDecimal("12.00")),
            new Connector(null, ConnectorType.TYPE2,  new BigDecimal("22.0"),  new BigDecimal("4.00"))
        );

        seedStation("Edinburgh Waverley", "Princes St", "Edinburgh",
            new BigDecimal("55.952100"), new BigDecimal("-3.190700"),
            new Connector(null, ConnectorType.CCS,    new BigDecimal("50.0"),  new BigDecimal("7.00")),
            new Connector(null, ConnectorType.CHADEMO,new BigDecimal("50.0"),  new BigDecimal("7.00"))
        );

        log.info("Seed complete. Stations: {}", stationRepository.count());
    }

    private void seedStation(String name, String address, String city,
                             BigDecimal lat, BigDecimal lng,
                             Connector... connectors) {
        ChargingStation s = new ChargingStation(name, address, city, lat, lng,
            name + " — operated by SmartCharge. Open 24/7.");
        for (Connector c : connectors) {
            c.setStation(s);
            c.setActive(true);
            s.getConnectors().add(c);
        }
        stationRepository.save(s);
    }
}
