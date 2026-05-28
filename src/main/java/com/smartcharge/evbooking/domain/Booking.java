package com.smartcharge.evbooking.domain;

import com.smartcharge.evbooking.domain.enums.BookingStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "connector_id", nullable = false)
    private Connector connector;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Booking() { /* JPA */ }

    public Booking(User user, Connector connector, Instant startTime, Instant endTime) {
        this.user = user;
        this.connector = connector;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = BookingStatus.CONFIRMED;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Connector getConnector() { return connector; }
    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }
    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }

    public boolean isOwnedBy(Long userId) {
        return user != null && Objects.equals(user.getId(), userId);
    }

    public boolean isInFuture(Instant now) {
        return startTime.isAfter(now);
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Booking b)) return false;
        return Objects.equals(id, b.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
}
