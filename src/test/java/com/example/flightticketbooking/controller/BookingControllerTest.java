package com.example.flightticketbooking.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.flightticketbooking.domain.Flight;
import com.example.flightticketbooking.repository.FlightRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class BookingControllerTest {

    private static final String FLIGHT_WITH_SEATS = "TEST-10";
    private static final String FLIGHT_ALMOST_FULL = "TEST-02";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FlightRepository flightRepository;

    @BeforeEach
    void resetFlights() {
        flightRepository.save(new Flight(FLIGHT_WITH_SEATS, 10));
        flightRepository.save(new Flight(FLIGHT_ALMOST_FULL, 2));
    }

    @Test
    void createsBookingAndReturns201() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"flightNumber":"TEST-10","passengerName":"Ada Lovelace","numberOfSeats":2}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingId").isNotEmpty())
                .andExpect(jsonPath("$.flightNumber").value(FLIGHT_WITH_SEATS))
                .andExpect(jsonPath("$.passengerName").value("Ada Lovelace"))
                .andExpect(jsonPath("$.numberOfSeats").value(2));

        assertEquals(8, flightRepository.findByFlightNumber(FLIGHT_WITH_SEATS).orElseThrow().getAvailableSeats());
    }

    @Test
    void returns400WhenRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"flightNumber":"","passengerName":"  ","numberOfSeats":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", Matchers.containsString("flightNumber must not be blank")))
                .andExpect(jsonPath("$.message", Matchers.containsString("passengerName must not be blank")))
                .andExpect(jsonPath("$.message", Matchers.containsString("numberOfSeats must be at least 1")));
    }

    @Test
    void returns404WhenFlightDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"flightNumber":"NOPE-999","passengerName":"Ada Lovelace","numberOfSeats":1}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message", Matchers.containsString("NOPE-999")));
    }

    @Test
    void returns409WhenSeatsAreInsufficient() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"flightNumber":"TEST-02","passengerName":"Ada Lovelace","numberOfSeats":3}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        assertEquals(2, flightRepository.findByFlightNumber(FLIGHT_ALMOST_FULL).orElseThrow().getAvailableSeats());
    }
}
