package com.example.flightticketbooking.repository;

import com.example.flightticketbooking.domain.Flight;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/** In-memory store of flights, keyed by flight number. */
@Repository
public class FlightRepository {

    private final Map<String, Flight> flightsByNumber = new ConcurrentHashMap<>();

    public void save(Flight flight) {
        flightsByNumber.put(flight.getFlightNumber(), flight);
    }

    public Optional<Flight> findByFlightNumber(String flightNumber) {
        return Optional.ofNullable(flightsByNumber.get(flightNumber));
    }
}
