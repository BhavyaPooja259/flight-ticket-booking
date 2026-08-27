package com.example.flightticketbooking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.flightticketbooking.domain.Flight;
import com.example.flightticketbooking.dto.BookingRequest;
import com.example.flightticketbooking.exception.InsufficientSeatsException;
import com.example.flightticketbooking.repository.BookingRepository;
import com.example.flightticketbooking.repository.FlightRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BookingServiceConcurrencyTest {

    private static final int CAPACITY = 10;
    private static final int CONCURRENT_REQUESTS = 40;

    /**
     * A lost update only shows up on some runs, so the race is repeated a few
     * times to make the test a dependable guard against overbooking.
     */
    private static final int ROUNDS = 5;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    void concurrentBookingsNeverOverbookTheFlight() throws Exception {
        for (int round = 0; round < ROUNDS; round++) {
            bookOneSeatConcurrently("TEST-CONCURRENT-" + round);
        }
    }

    /**
     * Fires {@link #CONCURRENT_REQUESTS} single-seat bookings at a flight that
     * only has {@link #CAPACITY} seats, releasing every thread at the same
     * moment, and asserts that exactly the available seats were sold.
     */
    private void bookOneSeatConcurrently(String flightNumber) throws Exception {
        flightRepository.save(new Flight(flightNumber, CAPACITY));

        AtomicInteger booked = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        CyclicBarrier startLine = new CyclicBarrier(CONCURRENT_REQUESTS);
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);

        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
                String passengerName = "Passenger " + i;
                futures.add(executor.submit(() -> {
                    startLine.await();
                    try {
                        bookingService.book(new BookingRequest(flightNumber, passengerName, 1));
                        booked.incrementAndGet();
                    } catch (InsufficientSeatsException expectedOnceFull) {
                        rejected.incrementAndGet();
                    }
                    return null;
                }));
            }
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(CAPACITY, booked.get(), "only the available seats may be booked");
        assertEquals(CONCURRENT_REQUESTS - CAPACITY, rejected.get());
        assertEquals(0, flightRepository.findByFlightNumber(flightNumber).orElseThrow().getAvailableSeats());
        assertEquals(CAPACITY, bookingRepository.countByFlightNumber(flightNumber));
    }
}
