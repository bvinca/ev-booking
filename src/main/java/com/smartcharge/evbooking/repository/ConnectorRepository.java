package com.smartcharge.evbooking.repository;

import com.smartcharge.evbooking.domain.Connector;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConnectorRepository extends JpaRepository<Connector, Long> {

    /** Eagerly fetches the parent station so templates can read connector.station.* safely. */
    @Override
    @EntityGraph(attributePaths = "station")
    Optional<Connector> findById(Long id);

    List<Connector> findByStationIdOrderByIdAsc(Long stationId);

    /**
     * Pessimistic row lock used during booking creation. The lock is held for the
     * duration of the surrounding transaction and forces concurrent transactions
     * attempting to book the same connector to queue behind the holder, preventing
     * lost-update / phantom races on the overlap check.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Connector c WHERE c.id = :id")
    Optional<Connector> findByIdForUpdate(@Param("id") Long id);
}
