package com.busreservation.bus_reservation.service;

import com.busreservation.bus_reservation.dto.ExcelImportDto;
import com.busreservation.bus_reservation.model.*;
import com.busreservation.bus_reservation.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ExcelImportService {

    private final BusRepository busRepository;
    private final TripRepository tripRepository;
    private final TripStopRepository tripStopRepository;
    private final SeatRepository seatRepository;
    private final FareRateRepository fareRateRepository;
    private final DayCalendarRepository dayCalendarRepository;

    // Pattern for parsing stops: StopName(arrive-depart)[km]
    private static final Pattern STOP_PATTERN = Pattern.compile(
        "([^(]+)(?:\\(([^)]+)\\))?\\[(\\d+)km\\]"
    );

    public ExcelImportService(BusRepository busRepository, TripRepository tripRepository,
                             TripStopRepository tripStopRepository, SeatRepository seatRepository,
                             FareRateRepository fareRateRepository, DayCalendarRepository dayCalendarRepository) {
        this.busRepository = busRepository;
        this.tripRepository = tripRepository;
        this.tripStopRepository = tripStopRepository;
        this.seatRepository = seatRepository;
        this.fareRateRepository = fareRateRepository;
        this.dayCalendarRepository = dayCalendarRepository;
    }

    @Transactional
    public ExcelImportDto.ImportResponse importExcel(MultipartFile file) {
        String batchId = "BATCH_" + System.currentTimeMillis();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int totalRows = 0;
        int successfulRows = 0;
        int skippedRows = 0;
        int maintenanceRows = 0;

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheet("31Day_Master_Plan");
            if (sheet == null) {
                errors.add("Sheet '31Day_Master_Plan' not found");
                return buildErrorResponse(errors);
            }

            // Initialize fare rates if not exists
            initializeFareRates();
            initializeDayCalendar();

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                errors.add("Header row not found");
                return buildErrorResponse(errors);
            }

            Map<String, Integer> columnMap = buildColumnMap(headerRow);
            if (!validateColumns(columnMap, errors)) {
                return buildErrorResponse(errors);
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                totalRows++;
                try {
                    ExcelImportDto.ExcelRow excelRow = parseRow(row, columnMap);
                    if (excelRow == null) {
                        skippedRows++;
                        continue;
                    }

                    if ("Maintenance".equalsIgnoreCase(excelRow.getMaintenanceStatus())) {
                        maintenanceRows++;
                    }

                    processRow(excelRow, batchId, warnings);
                    successfulRows++;

                } catch (Exception e) {
                    errors.add("Row " + (i + 1) + ": " + e.getMessage());
                    skippedRows++;
                }
            }

        } catch (IOException e) {
            errors.add("Failed to read Excel file: " + e.getMessage());
            return buildErrorResponse(errors);
        }

        ExcelImportDto.ImportSummary summary = ExcelImportDto.ImportSummary.builder()
                .totalRows(totalRows)
                .successfulRows(successfulRows)
                .skippedRows(skippedRows)
                .maintenanceRows(maintenanceRows)
                .batchId(batchId)
                .build();

        return ExcelImportDto.ImportResponse.builder()
                .success(true)
                .message("Import completed successfully")
                .summary(summary)
                .warnings(warnings)
                .errors(errors)
                .build();
    }

    private void initializeFareRates() {
        Map<String, Double> rates = Map.of(
                "Non-AC Seater", 1.00,
                "Non-AC Sleeper", 1.20,
                "AC Seater", 1.50,
                "AC Sleeper", 2.00
        );

        rates.forEach((category, rate) -> {
            if (!fareRateRepository.existsById(category)) {
                FareRate fareRate = FareRate.builder()
                        .category(category)
                        .ratePerKm(java.math.BigDecimal.valueOf(rate))
                        .build();
                fareRateRepository.save(fareRate);
            }
        });
    }

    private void initializeDayCalendar() {
        for (int day = 1; day <= 31; day++) {
            if (!dayCalendarRepository.existsById(day)) {
                DayCalendar dayCalendar = DayCalendar.builder()
                        .dayNo(day)
                        .label("Day " + day)
                        .build();
                dayCalendarRepository.save(dayCalendar);
            }
        }
    }

    private Map<String, Integer> buildColumnMap(Row headerRow) {
        Map<String, Integer> columnMap = new HashMap<>();
        for (Cell cell : headerRow) {
            if (cell.getCellType() == CellType.STRING) {
                columnMap.put(cell.getStringCellValue().trim(), cell.getColumnIndex());
            }
        }
        return columnMap;
    }

    private boolean validateColumns(Map<String, Integer> columnMap, List<String> errors) {
        String[] requiredColumns = {
                "Date", "Day", "BusID", "BusType", "Capacity", "FromCity",
                "DepartureTime", "StopsWithTimings", "Destination", "ArrivalTime",
                "TotalKM", "FarePerSeat", "SeatsAvailable", "MaintenanceStatus"
        };

        boolean valid = true;
        for (String column : requiredColumns) {
            if (!columnMap.containsKey(column)) {
                errors.add("Required column missing: " + column);
                valid = false;
            }
        }
        return valid;
    }

    private ExcelImportDto.ExcelRow parseRow(Row row, Map<String, Integer> columnMap) {
        try {
            return ExcelImportDto.ExcelRow.builder()
                    .date(getCellStringValue(row, columnMap.get("Date")))
                    .day(getCellIntValue(row, columnMap.get("Day")))
                    .busId(getCellStringValue(row, columnMap.get("BusID")))
                    .busType(getCellStringValue(row, columnMap.get("BusType")))
                    .capacity(getCellIntValue(row, columnMap.get("Capacity")))
                    .fromCity(getCellStringValue(row, columnMap.get("FromCity")))
                    .departureTime(parseTime(getCellStringValue(row, columnMap.get("DepartureTime"))))
                    .stopsWithTimings(getCellStringValue(row, columnMap.get("StopsWithTimings")))
                    .destination(getCellStringValue(row, columnMap.get("Destination")))
                    .arrivalTime(parseTime(getCellStringValue(row, columnMap.get("ArrivalTime"))))
                    .totalKm(getCellIntValue(row, columnMap.get("TotalKM")))
                    .farePerSeat(getCellIntValue(row, columnMap.get("FarePerSeat")))
                    .seatsAvailable(getCellIntValue(row, columnMap.get("SeatsAvailable")))
                    .maintenanceStatus(getCellStringValue(row, columnMap.get("MaintenanceStatus")))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse row: " + e.getMessage());
        }
    }

    private void processRow(ExcelImportDto.ExcelRow excelRow, String batchId, List<String> warnings) {
        // Create or update bus
        Bus bus = busRepository.findById(excelRow.getBusId()).orElse(null);
        if (bus == null) {
            bus = Bus.builder()
                    .busId(excelRow.getBusId())
                    .busType(excelRow.getBusType())
                    .capacity(excelRow.getCapacity())
                    .build();
            bus = busRepository.save(bus);

            // Create seats for the bus
            createSeatsForBus(bus);
        }

        // Parse stops
        List<ExcelImportDto.ParsedStop> parsedStops = parseStopsWithTimings(excelRow.getStopsWithTimings());
        
        // Validate total KM
        if (!parsedStops.isEmpty()) {
            int lastKm = parsedStops.get(parsedStops.size() - 1).getCumulativeKm();
            if (!lastKm.equals(excelRow.getTotalKm())) {
                warnings.add("TotalKM mismatch for " + excelRow.getBusId() + " Day " + excelRow.getDay() + 
                           ": Excel=" + excelRow.getTotalKm() + ", Calculated=" + lastKm);
            }
        }

        // Create or update trip
        Trip existingTrip = tripRepository.findByDayNoAndBusId(excelRow.getDay(), excelRow.getBusId());
        Trip trip;
        
        if (existingTrip != null) {
            // Update existing trip
            trip = existingTrip;
            trip.setFromCity(excelRow.getFromCity());
            trip.setToCity(excelRow.getDestination());
            trip.setDepartureTime(excelRow.getDepartureTime());
            trip.setArrivalTime(excelRow.getArrivalTime());
            trip.setTotalKm(excelRow.getTotalKm());
            trip.setPrice(excelRow.getFarePerSeat() != null ? excelRow.getFarePerSeat() : 0);
            trip.setStatus(excelRow.getMaintenanceStatus());
            trip.setImportBatchId(batchId);
            
            // Clear existing stops
            tripStopRepository.deleteByTripId(trip.getId());
        } else {
            // Create new trip
            trip = Trip.builder()
                    .dayNo(excelRow.getDay())
                    .bus(bus)
                    .fromCity(excelRow.getFromCity())
                    .toCity(excelRow.getDestination())
                    .departureTime(excelRow.getDepartureTime())
                    .arrivalTime(excelRow.getArrivalTime())
                    .totalKm(excelRow.getTotalKm())
                    .price(excelRow.getFarePerSeat() != null ? excelRow.getFarePerSeat() : 0)
                    .status(excelRow.getMaintenanceStatus())
                    .importBatchId(batchId)
                    .build();
        }
        
        trip = tripRepository.save(trip);

        // Create trip stops
        for (ExcelImportDto.ParsedStop parsedStop : parsedStops) {
            TripStop tripStop = TripStop.builder()
                    .trip(trip)
                    .seqNo(parsedStop.getSequence())
                    .stopName(parsedStop.getStopName())
                    .arriveTime(parsedStop.getArriveTime())
                    .departTime(parsedStop.getDepartTime())
                    .cumulativeKm(parsedStop.getCumulativeKm())
                    .build();
            tripStopRepository.save(tripStop);
        }
    }

    private void createSeatsForBus(Bus bus) {
        // Create seats based on bus type and capacity
        for (int i = 1; i <= bus.getCapacity(); i++) {
            Seat seat = Seat.builder()
                    .bus(bus)
                    .seatNo(String.valueOf(i))
                    .build();
            seatRepository.save(seat);
        }
    }

    private List<ExcelImportDto.ParsedStop> parseStopsWithTimings(String stopsWithTimings) {
        List<ExcelImportDto.ParsedStop> stops = new ArrayList<>();
        
        if (stopsWithTimings == null || stopsWithTimings.trim().isEmpty()) {
            return stops;
        }

        String[] segments = stopsWithTimings.split("\\s*->\\s*");
        int sequence = 0;

        for (String segment : segments) {
            Matcher matcher = STOP_PATTERN.matcher(segment.trim());
            if (matcher.find()) {
                String stopName = matcher.group(1).trim();
                String timings = matcher.group(2); // arrive-depart or just one time
                int cumulativeKm = Integer.parseInt(matcher.group(3));

                LocalTime arriveTime = null;
                LocalTime departTime = null;

                if (timings != null && !timings.trim().isEmpty()) {
                    if (timings.contains("-")) {
                        String[] times = timings.split("-");
                        if (times.length == 2) {
                            arriveTime = parseTime(times[0].trim());
                            departTime = parseTime(times[1].trim());
                        }
                    } else {
                        // Single time - could be arrival (for destination) or departure (for origin)
                        if (sequence == 0) {
                            departTime = parseTime(timings.trim());
                        } else {
                            arriveTime = parseTime(timings.trim());
                        }
                    }
                }

                stops.add(ExcelImportDto.ParsedStop.builder()
                        .stopName(stopName)
                        .arriveTime(arriveTime)
                        .departTime(departTime)
                        .cumulativeKm(cumulativeKm)
                        .sequence(sequence)
                        .build());

                sequence++;
            }
        }

        return stops;
    }

    private LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Handle both HH:mm and H:mm formats
            if (timeStr.length() == 4 && timeStr.charAt(1) == ':') {
                timeStr = "0" + timeStr; // Convert H:mm to HH:mm
            }
            return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            throw new RuntimeException("Invalid time format: " + timeStr);
        }
    }

    private String getCellStringValue(Row row, Integer columnIndex) {
        if (columnIndex == null) return null;
        Cell cell = row.getCell(columnIndex);
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf((int) cell.getNumericCellValue());
            default:
                return null;
        }
    }

    private Integer getCellIntValue(Row row, Integer columnIndex) {
        if (columnIndex == null) return null;
        Cell cell = row.getCell(columnIndex);
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case NUMERIC:
                return (int) cell.getNumericCellValue();
            case STRING:
                try {
                    return Integer.parseInt(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    return null;
                }
            default:
                return null;
        }
    }

    private ExcelImportDto.ImportResponse buildErrorResponse(List<String> errors) {
        return ExcelImportDto.ImportResponse.builder()
                .success(false)
                .message("Import failed")
                .errors(errors)
                .build();
    }
}