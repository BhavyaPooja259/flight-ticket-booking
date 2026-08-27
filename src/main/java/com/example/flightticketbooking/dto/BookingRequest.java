package com.example.flightticketbooking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record BookingRequest(
        @NotBlank(message = "flightNumber must not be blank")
        String flightNumber,

        @NotBlank(message = "passengerName must not be blank")
        String passengerName,

        @Min(value = 1, message = "numberOfSeats must be at least 1")
        int numberOfSeats) {
}
