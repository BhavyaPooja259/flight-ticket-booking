package com.example.flightticketbooking.dto;

public record ErrorResponse(int status, String error, String message) {
}
