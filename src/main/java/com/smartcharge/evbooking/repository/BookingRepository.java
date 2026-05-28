package com.smartcharge.evbooking.repository;

import com.smartcharge.evbooking.domain.Booking;
import com.smartcharge.evbooking.domain.enums.BookingStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * Find any CONFIRMED booking on a given connector that overlaps the
     * proposed [start, end) interval. Two intervals overlap iff
     * {@code existing.start < new.end AND existing.end > new.start}.
     *
     * <p>An optional {@code excludeId} parameter lets the modify-booking flow
     * exclude the row being changed from its own overlap check.</p>
     */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.connector.id = :connectorId
          AND b.status = com.smartcharge.evbooking.domain.enums.BookingStatus.CONFIRMED
          AND b.startTime < :end
          AND b.endTime   > :start
          AND (:excludeId IS NULL OR b.id <> :excludeId)
        """)
    List<Booking> findOverlapping(@Param("connectorId") Long connectorId,
                                  @Param("start") Instant start,
                                  @Param("end") Instant end,
                                  @Param("excludeId") Long excludeId);

    // The EntityGraph annotations eagerly fetch user, connector, and station
    // so templates can navigate booking.user.* and booking.connector.station.*
    // without triggering LazyInitializationException (open-in-view is off).

    @Override
    @EntityGraph(attributePaths = {"user", "connector", "connector.station"})
    Optional<Booking> findById(Long id);

    @EntityGraph(attributePaths = {"user", "connector", "connector.station"})
    List<Booking> findByUserIdOrderByStartTimeDesc(Long userId);

    @EntityGraph(attributePaths = {"user", "connector", "connector.station"})
    List<Booking> findByUserIdAndStatusOrderByStartTimeAsc(Long userId, BookingStatus status);

    List<Booking> findByConnectorIdAndStatusAndStartTimeBetweenOrderByStartTimeAsc(
        Long connectorId, BookingStatus status, Instant from, Instant to);

    @EntityGraph(attributePaths = {"user", "connector", "connector.station"})
    @Query("SELECT b FROM Booking b ORDER BY b.startTime DESC")
    List<Booking> findAllOrderedByStartDesc();
}
