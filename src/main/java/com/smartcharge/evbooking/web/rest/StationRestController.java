package com.smartcharge.evbooking.web.rest;

import com.smartcharge.evbooking.domain.ChargingStation;
import com.smartcharge.evbooking.domain.Connector;
import com.smartcharge.evbooking.domain.enums.ConnectorType;
import com.smartcharge.evbooking.service.StationService;
import com.smartcharge.evbooking.web.dto.request.ConnectorRequest;
import com.smartcharge.evbooking.web.dto.request.StationRequest;
import com.smartcharge.evbooking.web.dto.response.ConnectorDto;
import com.smartcharge.evbooking.web.dto.response.StationDto;
import com.smartcharge.evbooking.web.mapper.StationMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/stations")
public class StationRestController {

    private final StationService stationService;
    private final StationMapper mapper;

    public StationRestController(StationService stationService, StationMapper mapper) {
        this.stationService = stationService;
        this.mapper = mapper;
    }

    @GetMapping
    public List<StationDto> list(@RequestParam(required = false) String city,
                                 @RequestParam(required = false) ConnectorType type,
                                 @RequestParam(required = false) BigDecimal minKw) {
        return stationService.search(city, type, minKw).stream().map(mapper::toDto).toList();
    }

    @GetMapping("/{id}")
    public StationDto get(@PathVariable Long id) {
        return mapper.toDto(stationService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StationDto> create(@Valid @RequestBody StationRequest req) {
        ChargingStation s = stationService.create(req);
        return ResponseEntity
            .created(URI.create("/api/v1/stations/" + s.getId()))
            .body(mapper.toDto(s));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public StationDto update(@PathVariable Long id, @Valid @RequestBody StationRequest req) {
        return mapper.toDto(stationService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        stationService.delete(id);
    }

    @PostMapping("/{id}/connectors")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ConnectorDto> addConnector(@PathVariable Long id,
                                                     @Valid @RequestBody ConnectorRequest req) {
        Connector c = stationService.addConnector(id, req);
        return ResponseEntity
            .created(URI.create("/api/v1/connectors/" + c.getId()))
            .body(mapper.toDto(c));
    }
}
