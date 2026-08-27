package com.example.flightticketbooking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Incoming booking request.
 *
 * <p>{@code numberOfSeats} is an {@link Integer} rather than an {@code int} so
 * that a missing or {@code null} value is reported as "is required" instead of
 * silently defaulting to zero. Surrounding whitespace is trimmed before
 * validation, so a value of {@code "   "} is rejected as blank and
 * {@code " AI101 "} still resolves to flight {@code AI101}.
 */
public record BookingRequest(
        @NotBlank(message = "must not be blank")
        String flightNumber,

        @NotBlank(message = "must not be blank")
        String passengerName,

        @NotNull(message = "is required")
        @Min(value = 1, message = "must be at least 1")
        Integer numberOfSeats) {

    public BookingRequest {
        flightNumber = trimToNull(flightNumber);
        passengerName = trimToNull(passengerName);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
