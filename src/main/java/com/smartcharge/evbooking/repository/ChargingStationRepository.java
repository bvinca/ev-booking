package com.smartcharge.evbooking.repository;

import com.smartcharge.evbooking.domain.ChargingStation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * The {@link EntityGraph} annotations make the connectors collection load
 * eagerly with the station in a single round-trip, so Thymeleaf templates can
 * iterate {@code station.connectors} without triggering
 * {@code LazyInitializationException} (we have {@code spring.jpa.open-in-view=false}).
 */
@Repository
public interface ChargingStationRepository extends JpaRepository<ChargingStation, Long> {

    @Override
    @EntityGraph(attributePaths = "connectors")
    Optional<ChargingStation> findById(Long id);

    @Override
    @EntityGraph(attributePaths = "connectors")
    List<ChargingStation> findAll();

    @EntityGraph(attributePaths = "connectors")
    List<ChargingStation> findByCityIgnoreCaseOrderByNameAsc(String city);
}
