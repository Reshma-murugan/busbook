package com.busreservation.bus_reservation.dto;

import lombok.*;

import java.time.LocalTime;
import java.util.List;

public class ExcelImportDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ImportRequest {
        // Multipart file will be handled in controller
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ImportResponse {
        private boolean success;
        private String message;
        private ImportSummary summary;
        private List<String> warnings;
        private List<String> errors;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ImportSummary {
        private int totalRows;
        private int successfulRows;
        private int skippedRows;
        private int maintenanceRows;
        private String batchId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExcelRow {
        private String date;
        private Integer day;
        private String busId;
        private String busType;
        private Integer capacity;
        private String fromCity;
        private LocalTime departureTime;
        private String stopsWithTimings;
        private String destination;
        private LocalTime arrivalTime;
        private Integer totalKm;
        private Integer farePerSeat;
        private Integer seatsAvailable;
        private String maintenanceStatus;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ParsedStop {
        private String stopName;
        private LocalTime arriveTime;
        private LocalTime departTime;
        private Integer cumulativeKm;
        private Integer sequence;
    }
}