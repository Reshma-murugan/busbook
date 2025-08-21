package com.busreservation.bus_reservation.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class BookingDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BookingRequest {
        private Long tripId;
        private LocalDate date;
        private Integer fromStopSeq;
        private Integer toStopSeq;
        private String seatNo;
        private PassengerInfo passenger;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PassengerInfo {
        private String name;
        private String phone;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BookingResponse {
        private Long bookingId;
        private String pnr;
        private String status;
        private Integer fareAmount;
        private LocalDateTime bookedAt;
        private BookingDetails details;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BookingDetails {
        private String busId;
        private String busType;
        private LocalDate travelDate;
        private String fromStop;
        private String toStop;
        private String seatNo;
        private PassengerInfo passenger;
    }
}