package com.smartcharge.evbooking.web.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public class BookingRequest {

    @NotNull
    private Long connectorId;

    @NotNull
    private Instant startTime;

    @NotNull
    private Instant endTime;

    public Long getConnectorId() { return connectorId; }
    public void setConnectorId(Long connectorId) { this.connectorId = connectorId; }
    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }
    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }
}
