package com.smartcharge.evbooking.web.mapper;

import com.smartcharge.evbooking.domain.ChargingStation;
import com.smartcharge.evbooking.domain.Connector;
import com.smartcharge.evbooking.web.dto.response.ConnectorDto;
import com.smartcharge.evbooking.web.dto.response.StationDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class StationMapper {

    public StationDto toDto(ChargingStation s) {
        List<ConnectorDto> connectors = s.getConnectors().stream()
            .map(this::toDto)
            .collect(Collectors.toList());
        return new StationDto(
            s.getId(), s.getName(), s.getAddress(), s.getCity(),
            s.getLatitude(), s.getLongitude(), s.getDescription(),
            connectors
        );
    }

    public ConnectorDto toDto(Connector c) {
        return new ConnectorDto(
            c.getId(),
            c.getStation() != null ? c.getStation().getId() : null,
            c.getConnectorType(),
            c.getPowerKw(),
            c.getPricePerHour(),
            c.isActive()
        );
    }
}
