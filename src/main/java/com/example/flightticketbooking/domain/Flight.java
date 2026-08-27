package com.example.flightticketbooking.domain;

/**
 * A flight with a fixed capacity and a mutable number of available seats.
 *
 * <p>Seat reservation is guarded by the flight's own monitor so that the
 * availability check and the decrement happen atomically. The application runs
 * as a single instance, so this in-process lock is enough to prevent
 * overbooking; every booking for the same flight number goes through the same
 * {@code Flight} instance held by the repository.
 */
public class Flight {

    private final String flightNumber;
    private final int capacity;
    private int availableSeats;

    public Flight(String flightNumber, int capacity) {
        this.flightNumber = flightNumber;
        this.capacity = capacity;
        this.availableSeats = capacity;
    }

    /**
     * Atomically reserves {@code numberOfSeats} if enough seats are left.
     *
     * @return {@code true} if the seats were reserved, {@code false} if there
     *         were not enough seats available.
     */
    public synchronized boolean reserveSeats(int numberOfSeats) {
        if (numberOfSeats > availableSeats) {
            return false;
        }
        availableSeats -= numberOfSeats;
        return true;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public int getCapacity() {
        return capacity;
    }

    public synchronized int getAvailableSeats() {
        return availableSeats;
    }
}
