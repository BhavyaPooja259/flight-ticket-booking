package com.example.flightticketbooking.domain;

/** A confirmed booking of one or more seats on a flight. */
public record Booking(String bookingId, String flightNumber, String passengerName, int numberOfSeats) {
}
