package com.smartcharge.evbooking.web.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record StationDto(
    Long id,
    String name,
    String address,
    String city,
    BigDecimal latitude,
    BigDecimal longitude,
    String description,
    List<ConnectorDto> connectors
) {}
