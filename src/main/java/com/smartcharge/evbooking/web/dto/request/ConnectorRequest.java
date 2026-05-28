package com.smartcharge.evbooking.web.dto.request;

import com.smartcharge.evbooking.domain.enums.ConnectorType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class ConnectorRequest {

    @NotNull
    private ConnectorType connectorType;

    @NotNull @DecimalMin(value = "0.1")
    private BigDecimal powerKw;

    @NotNull @DecimalMin(value = "0.0")
    private BigDecimal pricePerHour;

    private boolean active = true;

    public ConnectorType getConnectorType() { return connectorType; }
    public void setConnectorType(ConnectorType connectorType) { this.connectorType = connectorType; }
    public BigDecimal getPowerKw() { return powerKw; }
    public void setPowerKw(BigDecimal powerKw) { this.powerKw = powerKw; }
    public BigDecimal getPricePerHour() { return pricePerHour; }
    public void setPricePerHour(BigDecimal pricePerHour) { this.pricePerHour = pricePerHour; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
