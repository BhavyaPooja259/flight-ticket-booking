package com.example.flightticketbooking.exception;

public class InsufficientSeatsException extends RuntimeException {

    public InsufficientSeatsException(String flightNumber, int requestedSeats) {
        super("Insufficient seats on flight " + flightNumber + " for " + requestedSeats + " seat(s)");
    }
}
