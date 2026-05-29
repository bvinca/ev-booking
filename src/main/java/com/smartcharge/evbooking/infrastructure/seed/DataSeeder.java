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

        // Six stations across Thessaloniki, Greece.
        seedStation("Aristotelous Square Hub", "Plateia Aristotelous", "Thessaloniki",
            new BigDecimal("40.632300"), new BigDecimal("22.941700"),
            new Connector(null, ConnectorType.CCS,    new BigDecimal("150.0"), new BigDecimal("0.42")),
            new Connector(null, ConnectorType.TYPE2,  new BigDecimal("22.0"),  new BigDecimal("0.18")),
            new Connector(null, ConnectorType.CHADEMO,new BigDecimal("50.0"),  new BigDecimal("0.35"))
        );

        seedStation("White Tower Waterfront", "Leoforos Nikis 30", "Thessaloniki",
            new BigDecimal("40.626100"), new BigDecimal("22.948600"),
            new Connector(null, ConnectorType.CCS,    new BigDecimal("50.0"),  new BigDecimal("0.34")),
            new Connector(null, ConnectorType.TYPE2,  new BigDecimal("22.0"),  new BigDecimal("0.18"))
        );

        seedStation("Ladadika Quarter", "Katouni 12", "Thessaloniki",
            new BigDecimal("40.635400"), new BigDecimal("22.935100"),
            new Connector(null, ConnectorType.TYPE2,  new BigDecimal("22.0"),  new BigDecimal("0.20")),
            new Connector(null, ConnectorType.TYPE2,  new BigDecimal("11.0"),  new BigDecimal("0.16"))
        );

        seedStation("HELEXPO Fairgrounds", "Egnatia 154", "Thessaloniki",
            new BigDecimal("40.619000"), new BigDecimal("22.959400"),
            new Connector(null, ConnectorType.CCS,    new BigDecimal("350.0"), new BigDecimal("0.55")),
            new Connector(null, ConnectorType.TESLA,  new BigDecimal("250.0"), new BigDecimal("0.48")),
            new Connector(null, ConnectorType.CCS,    new BigDecimal("150.0"), new BigDecimal("0.42"))
        );

        seedStation("Kalamaria Marina", "Megalou Alexandrou 1", "Kalamaria",
            new BigDecimal("40.583000"), new BigDecimal("22.953000"),
            new Connector(null, ConnectorType.CCS,    new BigDecimal("100.0"), new BigDecimal("0.38")),
            new Connector(null, ConnectorType.TYPE2,  new BigDecimal("22.0"),  new BigDecimal("0.18")),
            new Connector(null, ConnectorType.CHADEMO,new BigDecimal("50.0"),  new BigDecimal("0.34"))
        );

        seedStation("Mediterranean Cosmos Mall", "11ο χλμ Ε.Ο. Θεσσαλονίκης - Ν. Μουδανιών", "Pylaia",
            new BigDecimal("40.534500"), new BigDecimal("23.004000"),
            new Connector(null, ConnectorType.CCS,    new BigDecimal("180.0"), new BigDecimal("0.45")),
            new Connector(null, ConnectorType.TESLA,  new BigDecimal("250.0"), new BigDecimal("0.48")),
            new Connector(null, ConnectorType.TYPE2,  new BigDecimal("22.0"),  new BigDecimal("0.20"))
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
