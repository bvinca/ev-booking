package com.smartcharge.evbooking.web.mapper;

import com.smartcharge.evbooking.domain.Booking;
import com.smartcharge.evbooking.web.dto.response.BookingDto;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {

    public BookingDto toDto(Booking b) {
        return new BookingDto(
            b.getId(),
            b.getUser().getId(),
            b.getUser().getFullName(),
            b.getUser().getEmail(),
            b.getConnector().getId(),
            b.getConnector().getStation().getId(),
            b.getConnector().getStation().getName(),
            b.getConnector().getConnectorType().name(),
            b.getConnector().getPowerKw(),
            b.getStartTime(),
            b.getEndTime(),
            b.getStatus(),
            b.getCreatedAt()
        );
    }
}
