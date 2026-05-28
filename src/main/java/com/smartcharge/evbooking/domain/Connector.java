package com.smartcharge.evbooking.domain;

import com.smartcharge.evbooking.domain.enums.ConnectorType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "connectors")
public class Connector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "station_id", nullable = false)
    private ChargingStation station;

    @Enumerated(EnumType.STRING)
    @Column(name = "connector_type", nullable = false, length = 30)
    private ConnectorType connectorType;

    @Column(name = "power_kw", nullable = false, precision = 5, scale = 1)
    private BigDecimal powerKw;

    @Column(name = "price_per_hour", nullable = false, precision = 6, scale = 2)
    private BigDecimal pricePerHour;

    @Column(nullable = false)
    private boolean active = true;

    protected Connector() { /* JPA */ }

    public Connector(ChargingStation station, ConnectorType connectorType, BigDecimal powerKw, BigDecimal pricePerHour) {
        this.station = station;
        this.connectorType = connectorType;
        this.powerKw = powerKw;
        this.pricePerHour = pricePerHour;
    }

    public Long getId() { return id; }
    public ChargingStation getStation() { return station; }
    public void setStation(ChargingStation station) { this.station = station; }
    public ConnectorType getConnectorType() { return connectorType; }
    public void setConnectorType(ConnectorType connectorType) { this.connectorType = connectorType; }
    public BigDecimal getPowerKw() { return powerKw; }
    public void setPowerKw(BigDecimal powerKw) { this.powerKw = powerKw; }
    public BigDecimal getPricePerHour() { return pricePerHour; }
    public void setPricePerHour(BigDecimal pricePerHour) { this.pricePerHour = pricePerHour; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Connector that)) return false;
        return Objects.equals(id, that.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
}
