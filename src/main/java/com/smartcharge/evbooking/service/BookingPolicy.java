package com.smartcharge.evbooking.service;

import com.smartcharge.evbooking.service.exception.InvalidBookingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/**
 * Encapsulates booking validation rules so they are easy to test in isolation
 * and easy to change in one place.
 *
 * <p>Rules enforced:</p>
 * <ul>
 *   <li>{@code start} and {@code end} are aligned to the configured slot size (default 30 min).</li>
 *   <li>{@code end} is strictly after {@code start}.</li>
 *   <li>Duration is between 1 slot and {@code max-slots-per-booking} slots.</li>
 *   <li>{@code start} is in the future, and not more than {@code max-advance-days} ahead.</li>
 * </ul>
 */
@Component
public class BookingPolicy {

    private final int slotMinutes;
    private final int maxSlots;
    private final int maxAdvanceDays;

    public BookingPolicy(
        @Value("${app.booking.slot-minutes:30}") int slotMinutes,
        @Value("${app.booking.max-slots-per-booking:16}") int maxSlots,
        @Value("${app.booking.max-advance-days:60}") int maxAdvanceDays
    ) {
        if (slotMinutes <= 0 || 60 % slotMinutes != 0) {
            throw new IllegalArgumentException("slot-minutes must divide 60 evenly");
        }
        this.slotMinutes = slotMinutes;
        this.maxSlots = maxSlots;
        this.maxAdvanceDays = maxAdvanceDays;
    }

    public int getSlotMinutes() { return slotMinutes; }
    public int getMaxSlots()    { return maxSlots; }
    public int getMaxAdvanceDays() { return maxAdvanceDays; }

    public Duration getSlotDuration() {
        return Duration.ofMinutes(slotMinutes);
    }

    /**
     * Validate that {@code start} and {@code end} comply with all rules, relative to {@code now}.
     * Throws {@link InvalidBookingException} on the first failure.
     */
    public void validateRange(Instant start, Instant end, Instant now) {
        if (start == null || end == null) {
            throw new InvalidBookingException("Start and end time are required.");
        }
        if (!end.isAfter(start)) {
            throw new InvalidBookingException("End time must be after start time.");
        }
        if (!isAlignedToSlot(start) || !isAlignedToSlot(end)) {
            throw new InvalidBookingException(
                "Start and end must be aligned to " + slotMinutes + "-minute slots.");
        }
        long minutes = Duration.between(start, end).toMinutes();
        if (minutes % slotMinutes != 0) {
            throw new InvalidBookingException(
                "Duration must be a whole number of " + slotMinutes + "-minute slots.");
        }
        long slots = minutes / slotMinutes;
        if (slots < 1) {
            throw new InvalidBookingException("Booking must cover at least one slot.");
        }
        if (slots > maxSlots) {
            throw new InvalidBookingException(
                "Booking cannot exceed " + maxSlots + " slots (" + (maxSlots * slotMinutes / 60) + " hours).");
        }
        if (!start.isAfter(now)) {
            throw new InvalidBookingException("Start time must be in the future.");
        }
        Instant maxAdvance = now.plus(maxAdvanceDays, ChronoUnit.DAYS);
        if (start.isAfter(maxAdvance)) {
            throw new InvalidBookingException(
                "Cannot book more than " + maxAdvanceDays + " days in advance.");
        }
    }

    /** True iff the instant lies exactly on a slot boundary in UTC. */
    public boolean isAlignedToSlot(Instant t) {
        var ldt = t.atOffset(ZoneOffset.UTC).toLocalDateTime();
        return ldt.getSecond() == 0
            && ldt.getNano() == 0
            && ldt.getMinute() % slotMinutes == 0;
    }
}
