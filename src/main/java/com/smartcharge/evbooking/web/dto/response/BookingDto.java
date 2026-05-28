package com.smartcharge.evbooking.web.dto.response;

import com.smartcharge.evbooking.domain.enums.BookingStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record BookingDto(
    Long id,
    Long userId,
    String userFullName,
    String userEmail,
    Long connectorId,
    Long stationId,
    String stationName,
    String connectorType,
    BigDecimal connectorPowerKw,
    Instant startTime,
    Instant endTime,
    BookingStatus status,
    Instant createdAt
) {}
