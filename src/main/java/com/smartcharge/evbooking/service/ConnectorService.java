package com.smartcharge.evbooking.service;

import com.smartcharge.evbooking.domain.Connector;
import com.smartcharge.evbooking.repository.ConnectorRepository;
import com.smartcharge.evbooking.service.exception.ResourceNotFoundException;
import com.smartcharge.evbooking.web.dto.request.ConnectorRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ConnectorService {

    private final ConnectorRepository connectorRepository;

    public ConnectorService(ConnectorRepository connectorRepository) {
        this.connectorRepository = connectorRepository;
    }

    @Transactional(readOnly = true)
    public Connector findById(Long id) {
        return connectorRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Connector", id));
    }

    public Connector update(Long id, ConnectorRequest req) {
        Connector c = findById(id);
        c.setConnectorType(req.getConnectorType());
        c.setPowerKw(req.getPowerKw());
        c.setPricePerHour(req.getPricePerHour());
        c.setActive(req.isActive());
        return c;
    }

    public void delete(Long id) {
        if (!connectorRepository.existsById(id)) {
            throw new ResourceNotFoundException("Connector", id);
        }
        connectorRepository.deleteById(id);
    }
}
