package com.smartcharge.evbooking.web.dto.response;

import com.smartcharge.evbooking.domain.enums.ConnectorType;

import java.math.BigDecimal;

public record ConnectorDto(
    Long id,
    Long stationId,
    ConnectorType connectorType,
    BigDecimal powerKw,
    BigDecimal pricePerHour,
    boolean active
) {}
