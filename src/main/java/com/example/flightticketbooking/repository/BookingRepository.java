package com.example.flightticketbooking.repository;

import com.example.flightticketbooking.domain.Booking;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/** In-memory store of bookings, keyed by booking id. */
@Repository
public class BookingRepository {

    private final Map<String, Booking> bookingsById = new ConcurrentHashMap<>();

    public Booking save(Booking booking) {
        bookingsById.put(booking.bookingId(), booking);
        return booking;
    }

    public long countByFlightNumber(String flightNumber) {
        return bookingsById.values().stream()
                .filter(booking -> booking.flightNumber().equals(flightNumber))
                .count();
    }
}
