package com.busreservation.bus_reservation.controller;

import com.busreservation.bus_reservation.dto.BookingDto;
import com.busreservation.bus_reservation.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<BookingDto.BookingResponse> createBooking(
            Authentication authentication,
            @RequestBody BookingDto.BookingRequest request
    ) {
        String userEmail = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(bookingService.createBooking(userEmail, request));
    }

    @GetMapping("/{pnr}")
    public ResponseEntity<BookingDto.BookingResponse> getBookingByPnr(
            @PathVariable String pnr
    ) {
        return ResponseEntity.ok(bookingService.getBookingByPnr(pnr));
    }
}