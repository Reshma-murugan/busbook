package com.busreservation.bus_reservation.controller;

import com.busreservation.bus_reservation.dto.TripSearchDto;
import com.busreservation.bus_reservation.service.TripSearchService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/trips")
public class TripController {

    private final TripSearchService tripSearchService;

    public TripController(TripSearchService tripSearchService) {
        this.tripSearchService = tripSearchService;
    }

    @GetMapping("/search")
    public ResponseEntity<TripSearchDto.SearchResponse> searchTrips(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") Integer seats
    ) {
        TripSearchDto.SearchRequest request = TripSearchDto.SearchRequest.builder()
                .date(date)
                .from(from)
                .to(to)
                .category(category)
                .seats(seats)
                .build();

        return ResponseEntity.ok(tripSearchService.searchTrips(request));
    }

    @GetMapping("/{tripId}/availability")
    public ResponseEntity<TripSearchDto.SeatAvailabilityResponse> getSeatAvailability(
            @PathVariable Long tripId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam Integer fromSeq,
            @RequestParam Integer toSeq
    ) {
        TripSearchDto.SeatAvailabilityRequest request = TripSearchDto.SeatAvailabilityRequest.builder()
                .tripId(tripId)
                .date(date)
                .fromSeq(fromSeq)
                .toSeq(toSeq)
                .build();

        return ResponseEntity.ok(tripSearchService.getSeatAvailability(request));
    }
}