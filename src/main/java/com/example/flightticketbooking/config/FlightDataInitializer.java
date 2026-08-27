package com.example.flightticketbooking.config;

import com.example.flightticketbooking.domain.Flight;
import com.example.flightticketbooking.repository.FlightRepository;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Pre-populates the in-memory flight store at startup. Flight creation and
 * flight search are out of scope for this assignment.
 */
@Component
public class FlightDataInitializer implements CommandLineRunner {

    private final FlightRepository flightRepository;

    public FlightDataInitializer(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }

    @Override
    public void run(String... args) {
        List.of(
                new Flight("AI101", 150),
                new Flight("AI202", 60),
                new Flight("AI303", 2)
        ).forEach(flightRepository::save);
    }
}
