package com.example.flightticketbooking.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.flightticketbooking.domain.Flight;
import com.example.flightticketbooking.repository.FlightRepository;
import java.util.stream.Stream;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
    void trimsSurroundingWhitespaceBeforeBooking() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"flightNumber":"  TEST-10  ","passengerName":"  Ada Lovelace  ","numberOfSeats":1}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.flightNumber").value(FLIGHT_WITH_SEATS))
                .andExpect(jsonPath("$.passengerName").value("Ada Lovelace"));
    }

    static Stream<Arguments> invalidRequests() {
        return Stream.of(
                arguments("missing flightNumber",
                        """
                        {"passengerName":"Ada Lovelace","numberOfSeats":1}""",
                        "flightNumber", "must not be blank"),
                arguments("null flightNumber",
                        """
                        {"flightNumber":null,"passengerName":"Ada Lovelace","numberOfSeats":1}""",
                        "flightNumber", "must not be blank"),
                arguments("blank flightNumber",
                        """
                        {"flightNumber":"   ","passengerName":"Ada Lovelace","numberOfSeats":1}""",
                        "flightNumber", "must not be blank"),
                arguments("missing passengerName",
                        """
                        {"flightNumber":"TEST-10","numberOfSeats":1}""",
                        "passengerName", "must not be blank"),
                arguments("null passengerName",
                        """
                        {"flightNumber":"TEST-10","passengerName":null,"numberOfSeats":1}""",
                        "passengerName", "must not be blank"),
                arguments("blank passengerName",
                        """
                        {"flightNumber":"TEST-10","passengerName":"   ","numberOfSeats":1}""",
                        "passengerName", "must not be blank"),
                arguments("zero numberOfSeats",
                        """
                        {"flightNumber":"TEST-10","passengerName":"Ada Lovelace","numberOfSeats":0}""",
                        "numberOfSeats", "must be at least 1"),
                arguments("negative numberOfSeats",
                        """
                        {"flightNumber":"TEST-10","passengerName":"Ada Lovelace","numberOfSeats":-3}""",
                        "numberOfSeats", "must be at least 1"),
                arguments("missing numberOfSeats",
                        """
                        {"flightNumber":"TEST-10","passengerName":"Ada Lovelace"}""",
                        "numberOfSeats", "is required"),
                arguments("null numberOfSeats",
                        """
                        {"flightNumber":"TEST-10","passengerName":"Ada Lovelace","numberOfSeats":null}""",
                        "numberOfSeats", "is required"));
    }

    @ParameterizedTest(name = "{0} is rejected with 400")
    @MethodSource("invalidRequests")
    void returns400ForInvalidRequest(String description, String body, String field, String fieldMessage)
            throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.fieldErrors." + field).value(fieldMessage))
                .andExpect(jsonPath("$.message", Matchers.containsString(field + " " + fieldMessage)))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());

        assertEquals(10, flightRepository.findByFlightNumber(FLIGHT_WITH_SEATS).orElseThrow().getAvailableSeats());
    }

    @Test
    void reportsEveryInvalidFieldAtOnce() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"flightNumber":"","passengerName":"  ","numberOfSeats":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.flightNumber").value("must not be blank"))
                .andExpect(jsonPath("$.fieldErrors.passengerName").value("must not be blank"))
                .andExpect(jsonPath("$.fieldErrors.numberOfSeats").value("must be at least 1"))
                .andExpect(jsonPath("$.message").value(
                        "flightNumber must not be blank, numberOfSeats must be at least 1, "
                                + "passengerName must not be blank"));
    }

    @Test
    void returns400ForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or malformed JSON"))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    @Test
    void returns400ForNonNumericNumberOfSeats() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"flightNumber":"TEST-10","passengerName":"Ada Lovelace","numberOfSeats":"two"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or malformed JSON"));
    }

    @Test
    void returns415WhenContentTypeIsNotJson() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("flightNumber=TEST-10"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.message").value("Content-Type must be application/json"));
    }

    @Test
    void returns405ForUnsupportedMethod() throws Exception {
        mockMvc.perform(get("/api/bookings"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void returns404ForUnmappedPath() throws Exception {
        mockMvc.perform(get("/definitely-not-a-route"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.trace").doesNotExist());
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
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Flight not found: NOPE-999"))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void returns409WhenSeatsAreInsufficient() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"flightNumber":"TEST-02","passengerName":"Ada Lovelace","numberOfSeats":3}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(
                        "Insufficient seats on flight TEST-02 for 3 seat(s)"))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist());

        assertEquals(2, flightRepository.findByFlightNumber(FLIGHT_ALMOST_FULL).orElseThrow().getAvailableSeats());
    }
}
