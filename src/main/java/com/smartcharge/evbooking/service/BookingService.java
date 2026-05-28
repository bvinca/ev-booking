package com.smartcharge.evbooking.service;

import com.smartcharge.evbooking.domain.Booking;
import com.smartcharge.evbooking.domain.Connector;
import com.smartcharge.evbooking.domain.User;
import com.smartcharge.evbooking.domain.enums.BookingStatus;
import com.smartcharge.evbooking.repository.BookingRepository;
import com.smartcharge.evbooking.repository.ConnectorRepository;
import com.smartcharge.evbooking.repository.UserRepository;
import com.smartcharge.evbooking.service.exception.BookingConflictException;
import com.smartcharge.evbooking.service.exception.ForbiddenOperationException;
import com.smartcharge.evbooking.service.exception.InvalidBookingException;
import com.smartcharge.evbooking.service.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Concurrency-safe booking management.
 *
 * <h2>Why the double-booking problem is hard</h2>
 * <p>The naive "check then insert" sequence is a textbook race:</p>
 * <pre>
 *   T1: SELECT overlaps -> none
 *                                   T2: SELECT overlaps -> none
 *   T1: INSERT booking [10:00,10:30]
 *                                   T2: INSERT booking [10:00,10:30]   ← both succeed!
 * </pre>
 *
 * <h2>How this service prevents it (defence in depth)</h2>
 * <ol>
 *   <li><b>Pessimistic row lock</b> via {@code SELECT ... FOR UPDATE} on the
 *       {@code connectors} row before reading or writing bookings. Every
 *       concurrent transaction wanting to book the same connector queues up
 *       behind the lock holder, so the overlap check and the insert that
 *       follows it are effectively atomic relative to other booking transactions.</li>
 *   <li><b>Re-check overlaps inside the locked transaction</b>. Because the
 *       lock serialises booking creation on this connector, the check is now
 *       authoritative: nothing can sneak in between check and insert.</li>
 *   <li><b>PostgreSQL EXCLUDE constraint</b> on bookings (see V1 migration)
 *       rejects any pair of CONFIRMED bookings for the same connector whose
 *       time ranges overlap. Even if the application logic were subverted
 *       (e.g. via direct SQL or a logic bug), the database refuses the
 *       second row and Spring surfaces a {@link DataIntegrityViolationException},
 *       which we translate into {@link BookingConflictException}.</li>
 * </ol>
 *
 * <p>This is the standard pattern for safe reservation systems on a single
 * relational database: <em>locking + check + constraint</em>. It is robust
 * across multiple application instances because the lock and the constraint
 * both live in the database.</p>
 */
@Service
@Transactional
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final ConnectorRepository connectorRepository;
    private final UserRepository userRepository;
    private final BookingPolicy policy;
    private final Clock clock;

    public BookingService(BookingRepository bookingRepository,
                          ConnectorRepository connectorRepository,
                          UserRepository userRepository,
                          BookingPolicy policy,
                          Clock clock) {
        this.bookingRepository = bookingRepository;
        this.connectorRepository = connectorRepository;
        this.userRepository = userRepository;
        this.policy = policy;
        this.clock = clock;
    }

    // --------------------------------------------------------------------
    // Create
    // --------------------------------------------------------------------

    /**
     * Create a new CONFIRMED booking for {@code userId} on {@code connectorId}.
     *
     * <p>Runs in a single transaction at READ_COMMITTED isolation. The pessimistic
     * lock on the connector row is what makes the overlap check race-free, not
     * the isolation level.</p>
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Booking createBooking(Long userId, Long connectorId, Instant start, Instant end) {
        Instant now = Instant.now(clock);
        policy.validateRange(start, end, now);

        // (1) Acquire the row lock on the connector. Concurrent transactions
        // trying to book this same connector will block here until we commit.
        Connector connector = connectorRepository.findByIdForUpdate(connectorId)
            .orElseThrow(() -> new ResourceNotFoundException("Connector", connectorId));

        if (!connector.isActive()) {
            throw new InvalidBookingException("Connector is not active and cannot be booked.");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // (2) Overlap re-check inside the locked transaction. Authoritative
        // because the lock prevents other booking transactions from inserting
        // a competing row on this connector while we hold it.
        List<Booking> overlaps = bookingRepository.findOverlapping(connectorId, start, end, null);
        if (!overlaps.isEmpty()) {
            throw new BookingConflictException(
                "The selected time slot conflicts with an existing booking on this connector.");
        }

        Booking booking = new Booking(user, connector, start, end);

        try {
            booking = bookingRepository.saveAndFlush(booking);
        } catch (DataIntegrityViolationException ex) {
            // (3) Final safety net: the EXCLUDE constraint rejected the insert.
            // Should be extremely rare given the pessimistic lock, but covers
            // any edge case (e.g. lock acquired against a stale connector row).
            log.warn("Booking insert rejected by DB constraint: {}", ex.getMessage());
            throw new BookingConflictException(
                "The selected time slot conflicts with an existing booking on this connector.");
        }

        log.info("Booking created id={} user={} connector={} start={} end={}",
            booking.getId(), userId, connectorId, start, end);
        return booking;
    }

    // --------------------------------------------------------------------
    // Modify
    // --------------------------------------------------------------------

    /**
     * Change the time window of a future CONFIRMED booking.
     * Connector cannot be changed (assignment requirement: cancel + rebook instead).
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Booking modifyBooking(Long bookingId, Long requesterUserId, boolean requesterIsAdmin,
                                 Instant newStart, Instant newEnd) {
        Instant now = Instant.now(clock);
        policy.validateRange(newStart, newEnd, now);

        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        assertOwnerOrAdmin(booking, requesterUserId, requesterIsAdmin);
        assertModifiable(booking, now);

        // Lock the connector row to make the overlap re-check race-free.
        Connector connector = connectorRepository.findByIdForUpdate(booking.getConnector().getId())
            .orElseThrow(() -> new ResourceNotFoundException("Connector", booking.getConnector().getId()));

        List<Booking> overlaps = bookingRepository.findOverlapping(
            connector.getId(), newStart, newEnd, booking.getId());
        if (!overlaps.isEmpty()) {
            throw new BookingConflictException(
                "The new time window conflicts with another booking on this connector.");
        }

        booking.setStartTime(newStart);
        booking.setEndTime(newEnd);
        try {
            bookingRepository.saveAndFlush(booking);
        } catch (DataIntegrityViolationException ex) {
            throw new BookingConflictException(
                "The new time window conflicts with another booking on this connector.");
        }
        log.info("Booking {} modified by user={} (admin={}) new=[{},{})",
            booking.getId(), requesterUserId, requesterIsAdmin, newStart, newEnd);
        return booking;
    }

    // --------------------------------------------------------------------
    // Cancel
    // --------------------------------------------------------------------

    public void cancelBooking(Long bookingId, Long requesterUserId, boolean requesterIsAdmin) {
        Instant now = Instant.now(clock);
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        assertOwnerOrAdmin(booking, requesterUserId, requesterIsAdmin);
        if (!requesterIsAdmin) {
            // Drivers may only cancel future bookings; admins may cancel any.
            assertModifiable(booking, now);
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return; // already cancelled — no-op
        }
        booking.setStatus(BookingStatus.CANCELLED);
        log.info("Booking {} cancelled by user={} (admin={})", bookingId, requesterUserId, requesterIsAdmin);
    }

    // --------------------------------------------------------------------
    // Queries
    // --------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Booking> findOwnBookings(Long userId) {
        return bookingRepository.findByUserIdOrderByStartTimeDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Booking> findAll() {
        return bookingRepository.findAllOrderedByStartDesc();
    }

    @Transactional(readOnly = true)
    public Booking findById(Long id) {
        return bookingRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", id));
    }

    // --------------------------------------------------------------------
    // Internal checks
    // --------------------------------------------------------------------

    private void assertOwnerOrAdmin(Booking booking, Long requesterUserId, boolean isAdmin) {
        if (isAdmin) return;
        if (!booking.isOwnedBy(requesterUserId)) {
            throw new ForbiddenOperationException("You do not own this booking.");
        }
    }

    private void assertModifiable(Booking booking, Instant now) {
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new InvalidBookingException("This booking has already been cancelled.");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new InvalidBookingException("Completed bookings cannot be modified.");
        }
        if (!booking.isInFuture(now)) {
            throw new InvalidBookingException(
                "Bookings can only be modified or cancelled before the reserved slot begins.");
        }
    }
}
