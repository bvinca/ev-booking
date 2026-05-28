package com.smartcharge.evbooking.service;

import com.smartcharge.evbooking.domain.Booking;
import com.smartcharge.evbooking.domain.Connector;
import com.smartcharge.evbooking.domain.enums.BookingStatus;
import com.smartcharge.evbooking.repository.BookingRepository;
import com.smartcharge.evbooking.repository.ConnectorRepository;
import com.smartcharge.evbooking.service.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Computes the free 30-minute slot windows on a connector for a given day.
 * Used by the booking creation page to render an availability grid.
 */
@Service
public class AvailabilityService {

    private final BookingRepository bookingRepository;
    private final ConnectorRepository connectorRepository;
    private final BookingPolicy policy;
    private final Clock clock;

    public AvailabilityService(BookingRepository bookingRepository,
                               ConnectorRepository connectorRepository,
                               BookingPolicy policy,
                               Clock clock) {
        this.bookingRepository = bookingRepository;
        this.connectorRepository = connectorRepository;
        this.policy = policy;
        this.clock = clock;
    }

    public record Slot(Instant start, Instant end, boolean free, boolean past) {}

    @Transactional(readOnly = true)
    public List<Slot> getSlotsForDay(Long connectorId, LocalDate date) {
        Connector connector = connectorRepository.findById(connectorId)
            .orElseThrow(() -> new ResourceNotFoundException("Connector", connectorId));

        Instant dayStart = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant dayEnd   = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant now      = Instant.now(clock);

        List<Booking> existing = bookingRepository
            .findByConnectorIdAndStatusAndStartTimeBetweenOrderByStartTimeAsc(
                connector.getId(), BookingStatus.CONFIRMED, dayStart.minusSeconds(1), dayEnd);

        var slotDuration = policy.getSlotDuration();
        List<Slot> result = new ArrayList<>();
        Instant cursor = dayStart;
        while (cursor.isBefore(dayEnd)) {
            Instant slotEnd = cursor.plus(slotDuration);
            boolean overlaps = false;
            for (Booking b : existing) {
                if (b.getStartTime().isBefore(slotEnd) && b.getEndTime().isAfter(cursor)) {
                    overlaps = true;
                    break;
                }
            }
            boolean past = !cursor.isAfter(now);
            result.add(new Slot(cursor, slotEnd, !overlaps && !past, past));
            cursor = slotEnd;
        }
        return result;
    }
}
