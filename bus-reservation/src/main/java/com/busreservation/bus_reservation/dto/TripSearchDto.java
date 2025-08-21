package com.busreservation.bus_reservation.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class TripSearchDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SearchRequest {
        private LocalDate date;
        private String from;
        private String to;
        private String category; // optional filter
        private Integer seats; // default 1
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SearchResponse {
        private LocalDate searchDate;
        private String fromStop;
        private String toStop;
        private List<TripResult> trips;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TripResult {
        private Long tripId;
        private String busId;
        private String busType;
        private String route;
        private LocalTime departureTime;
        private LocalTime arrivalTime;
        private LocalTime boardingTime;
        private LocalTime droppingTime;
        private Integer distanceKm;
        private Integer fareAmount;
        private Integer availableSeats;
        private Integer fromStopSeq;
        private Integer toStopSeq;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SeatAvailabilityRequest {
        private Long tripId;
        private LocalDate date;
        private Integer fromSeq;
        private Integer toSeq;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SeatAvailabilityResponse {
        private Long tripId;
        private LocalDate date;
        private Integer fromSeq;
        private Integer toSeq;
        private List<SeatInfo> seats;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SeatInfo {
        private String seatNo;
        private Boolean available;
        private String layout; // "window", "aisle", "middle"
    }
}