package com.example.flightticketbooking.dto;

import com.example.flightticketbooking.domain.Booking;

public record BookingResponse(String bookingId, String flightNumber, String passengerName, int numberOfSeats) {

    public static BookingResponse from(Booking booking) {
        return new BookingResponse(
                booking.bookingId(),
                booking.flightNumber(),
                booking.passengerName(),
                booking.numberOfSeats());
    }
}
