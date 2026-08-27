package com.example.flightticketbooking.controller;

import com.example.flightticketbooking.domain.Booking;
import com.example.flightticketbooking.dto.BookingRequest;
import com.example.flightticketbooking.dto.BookingResponse;
import com.example.flightticketbooking.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody BookingRequest request) {
        Booking booking = bookingService.book(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(BookingResponse.from(booking));
    }
}
