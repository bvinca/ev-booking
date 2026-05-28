package com.smartcharge.evbooking.web.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Used to change the start/end time of an existing future booking.
 * Connector cannot be changed — drivers must cancel and rebook to switch connector.
 */
public class BookingModifyRequest {

    @NotNull
    private Instant startTime;

    @NotNull
    private Instant endTime;

    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }
    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }
}
