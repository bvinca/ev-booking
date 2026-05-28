package com.smartcharge.evbooking.web.rest;

import com.smartcharge.evbooking.service.ConnectorService;
import com.smartcharge.evbooking.web.dto.request.ConnectorRequest;
import com.smartcharge.evbooking.web.dto.response.ConnectorDto;
import com.smartcharge.evbooking.web.mapper.StationMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/connectors")
public class ConnectorRestController {

    private final ConnectorService connectorService;
    private final StationMapper mapper;

    public ConnectorRestController(ConnectorService connectorService, StationMapper mapper) {
        this.connectorService = connectorService;
        this.mapper = mapper;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ConnectorDto update(@PathVariable Long id, @Valid @RequestBody ConnectorRequest req) {
        return mapper.toDto(connectorService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        connectorService.delete(id);
    }
}
