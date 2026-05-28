package com.smartcharge.evbooking.service;

import com.smartcharge.evbooking.domain.ChargingStation;
import com.smartcharge.evbooking.domain.Connector;
import com.smartcharge.evbooking.domain.enums.ConnectorType;
import com.smartcharge.evbooking.repository.ChargingStationRepository;
import com.smartcharge.evbooking.service.exception.ResourceNotFoundException;
import com.smartcharge.evbooking.web.dto.request.ConnectorRequest;
import com.smartcharge.evbooking.web.dto.request.StationRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class StationService {

    private final ChargingStationRepository stationRepository;

    public StationService(ChargingStationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    @Transactional(readOnly = true)
    public List<ChargingStation> findAll() {
        return stationRepository.findAll();
    }

    @Transactional(readOnly = true)
    public ChargingStation findById(Long id) {
        return stationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Station", id));
    }

    /**
     * Search/filter stations. All filters are optional; null/blank means "any".
     * Filtering is done in-memory after a coarse DB fetch to keep the code simple
     * for the project's expected catalogue size. For larger datasets this would
     * become a Criteria/Specification-based query.
     */
    @Transactional(readOnly = true)
    public List<ChargingStation> search(String city, ConnectorType type, BigDecimal minPowerKw) {
        List<ChargingStation> base = (city == null || city.isBlank())
            ? stationRepository.findAll()
            : stationRepository.findByCityIgnoreCaseOrderByNameAsc(city.trim());

        return base.stream()
            .filter(s -> s.getConnectors().stream().anyMatch(c -> {
                if (!c.isActive()) return false;
                if (type != null && c.getConnectorType() != type) return false;
                if (minPowerKw != null && c.getPowerKw().compareTo(minPowerKw) < 0) return false;
                return true;
            }) || (type == null && minPowerKw == null))
            .toList();
    }

    public ChargingStation create(StationRequest req) {
        ChargingStation s = new ChargingStation(
            req.getName().trim(), req.getAddress().trim(), req.getCity().trim(),
            req.getLatitude(), req.getLongitude(), req.getDescription());
        return stationRepository.save(s);
    }

    public ChargingStation update(Long id, StationRequest req) {
        ChargingStation s = findById(id);
        s.setName(req.getName().trim());
        s.setAddress(req.getAddress().trim());
        s.setCity(req.getCity().trim());
        s.setLatitude(req.getLatitude());
        s.setLongitude(req.getLongitude());
        s.setDescription(req.getDescription());
        return s;
    }

    public void delete(Long id) {
        if (!stationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Station", id);
        }
        stationRepository.deleteById(id);
    }

    public Connector addConnector(Long stationId, ConnectorRequest req) {
        ChargingStation s = findById(stationId);
        Connector c = new Connector(s, req.getConnectorType(), req.getPowerKw(), req.getPricePerHour());
        c.setActive(req.isActive());
        s.addConnector(c);
        return c;
    }
}
