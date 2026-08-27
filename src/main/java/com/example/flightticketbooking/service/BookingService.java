package com.example.flightticketbooking.service;

import com.example.flightticketbooking.domain.Booking;
import com.example.flightticketbooking.domain.Flight;
import com.example.flightticketbooking.dto.BookingRequest;
import com.example.flightticketbooking.exception.FlightNotFoundException;
import com.example.flightticketbooking.exception.InsufficientSeatsException;
import com.example.flightticketbooking.repository.BookingRepository;
import com.example.flightticketbooking.repository.FlightRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class BookingService {

    private final FlightRepository flightRepository;
    private final BookingRepository bookingRepository;

    public BookingService(FlightRepository flightRepository, BookingRepository bookingRepository) {
        this.flightRepository = flightRepository;
        this.bookingRepository = bookingRepository;
    }

    /**
     * Books seats on a flight.
     *
     * <p>The seat availability check and the decrement are performed atomically
     * by {@link Flight#reserveSeats(int)}, so concurrent requests for the same
     * flight can never take the flight past its capacity.
     *
     * <p>Reserving the seats and recording the booking are two separate steps,
     * so if the second one fails the reserved seats are given back before the
     * failure propagates. Without that, a failed save would leave seats
     * deducted for a booking that does not exist, and they could never be sold.
     */
    public Booking book(BookingRequest request) {
        Flight flight = flightRepository.findByFlightNumber(request.flightNumber())
                .orElseThrow(() -> new FlightNotFoundException(request.flightNumber()));

        if (!flight.reserveSeats(request.numberOfSeats())) {
            throw new InsufficientSeatsException(request.flightNumber(), request.numberOfSeats());
        }

        try {
            Booking booking = new Booking(
                    UUID.randomUUID().toString(),
                    flight.getFlightNumber(),
                    request.passengerName(),
                    request.numberOfSeats());
            return bookingRepository.save(booking);
        } catch (RuntimeException bookingNotRecorded) {
            flight.releaseSeats(request.numberOfSeats());
            throw bookingNotRecorded;
        }
    }
}
