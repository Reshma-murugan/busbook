package com.busreservation.bus_reservation.service;

import com.busreservation.bus_reservation.dto.TripSearchDto;
import com.busreservation.bus_reservation.exception.BadRequestException;
import com.busreservation.bus_reservation.model.*;
import com.busreservation.bus_reservation.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TripSearchService {

    private final TripRepository tripRepository;
    private final TripStopRepository tripStopRepository;
    private final BookingRepository bookingRepository;
    private final FareRateRepository fareRateRepository;
    private final SeatRepository seatRepository;

    public TripSearchService(TripRepository tripRepository, TripStopRepository tripStopRepository,
                           BookingRepository bookingRepository, FareRateRepository fareRateRepository,
                           SeatRepository seatRepository) {
        this.tripRepository = tripRepository;
        this.tripStopRepository = tripStopRepository;
        this.bookingRepository = bookingRepository;
        this.fareRateRepository = fareRateRepository;
        this.seatRepository = seatRepository;
    }

    public TripSearchDto.SearchResponse searchTrips(TripSearchDto.SearchRequest request) {
        // Validate date is not in the past
        validateFutureDate(request.getDate());

        // Map date to day number (1-31)
        int dayNo = request.getDate().getDayOfMonth();

        // Get all trips for this day
        List<Trip> trips = tripRepository.findByDayNoAndStatus(dayNo, "Running");

        List<TripSearchDto.TripResult> results = new ArrayList<>();

        for (Trip trip : trips) {
            // Get stops for this trip
            List<TripStop> stops = tripStopRepository.findByTripIdOrderBySeqNoAsc(trip.getId());
            
            // Find from and to stops
            TripStop fromStop = findStopByName(stops, request.getFrom());
            TripStop toStop = findStopByName(stops, request.getTo());

            if (fromStop != null && toStop != null && fromStop.getSeqNo() < toStop.getSeqNo()) {
                // Check if departure time is valid for today
                if (request.getDate().equals(LocalDate.now(ZoneId.of("Asia/Kolkata")))) {
                    LocalTime now = LocalTime.now(ZoneId.of("Asia/Kolkata"));
                    if (fromStop.getDepartTime() != null && fromStop.getDepartTime().isBefore(now)) {
                        continue; // Skip past departures for today
                    }
                }

                // Filter by category if specified
                if (request.getCategory() != null && !request.getCategory().isEmpty() && 
                    !trip.getBus().getBusType().equalsIgnoreCase(request.getCategory())) {
                    continue;
                }

                // Calculate available seats for this segment
                int availableSeats = calculateAvailableSeats(trip.getId(), request.getDate(), 
                                                           fromStop.getSeqNo(), toStop.getSeqNo());

                if (availableSeats >= request.getSeats()) {
                    // Calculate fare
                    int distanceKm = toStop.getCumulativeKm() - fromStop.getCumulativeKm();
                    int fareAmount = calculateFare(trip.getBus().getBusType(), distanceKm);

                    String route = trip.getFromCity() + " → " + trip.getToCity();

                    results.add(TripSearchDto.TripResult.builder()
                            .tripId(trip.getId())
                            .busId(trip.getBus().getBusId())
                            .busType(trip.getBus().getBusType())
                            .route(route)
                            .departureTime(trip.getDepartureTime())
                            .arrivalTime(trip.getArrivalTime())
                            .boardingTime(fromStop.getDepartTime())
                            .droppingTime(toStop.getArriveTime())
                            .distanceKm(distanceKm)
                            .fareAmount(fareAmount)
                            .availableSeats(availableSeats)
                            .fromStopSeq(fromStop.getSeqNo())
                            .toStopSeq(toStop.getSeqNo())
                            .build());
                }
            }
        }

        return TripSearchDto.SearchResponse.builder()
                .searchDate(request.getDate())
                .fromStop(request.getFrom())
                .toStop(request.getTo())
                .trips(results)
                .build();
    }

    public TripSearchDto.SeatAvailabilityResponse getSeatAvailability(TripSearchDto.SeatAvailabilityRequest request) {
        validateFutureDate(request.getDate());

        Trip trip = tripRepository.findById(request.getTripId())
                .orElseThrow(() -> new BadRequestException("Trip not found"));

        List<Seat> allSeats = seatRepository.findByBusId(trip.getBus().getBusId());
        Set<String> bookedSeats = getBookedSeatsForSegment(request.getTripId(), request.getDate(), 
                                                          request.getFromSeq(), request.getToSeq());

        List<TripSearchDto.SeatInfo> seatInfos = allSeats.stream()
                .map(seat -> TripSearchDto.SeatInfo.builder()
                        .seatNo(seat.getSeatNo())
                        .available(!bookedSeats.contains(seat.getSeatNo()))
                        .layout(determineSeatLayout(seat.getSeatNo(), trip.getBus().getBusType()))
                        .build())
                .collect(Collectors.toList());

        return TripSearchDto.SeatAvailabilityResponse.builder()
                .tripId(request.getTripId())
                .date(request.getDate())
                .fromSeq(request.getFromSeq())
                .toSeq(request.getToSeq())
                .seats(seatInfos)
                .build();
    }

    private void validateFutureDate(LocalDate date) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        if (date.isBefore(today)) {
            throw new BadRequestException("Cannot book for past dates");
        }
    }

    private TripStop findStopByName(List<TripStop> stops, String stopName) {
        return stops.stream()
                .filter(stop -> stop.getStopName().equalsIgnoreCase(stopName))
                .findFirst()
                .orElse(null);
    }

    private int calculateAvailableSeats(Long tripId, LocalDate date, Integer fromSeq, Integer toSeq) {
        Trip trip = tripRepository.findById(tripId).orElse(null);
        if (trip == null) return 0;

        Set<String> bookedSeats = getBookedSeatsForSegment(tripId, date, fromSeq, toSeq);
        return trip.getBus().getCapacity() - bookedSeats.size();
    }

    private Set<String> getBookedSeatsForSegment(Long tripId, LocalDate date, Integer fromSeq, Integer toSeq) {
        List<Booking> bookings = bookingRepository.findByTripIdAndBookingDateAndStatus(tripId, date, "CONFIRMED");
        Set<String> bookedSeats = new HashSet<>();

        for (Booking booking : bookings) {
            // Check if segments overlap: [fromSeq, toSeq) overlaps with [booking.fromSeq, booking.toSeq)
            if (fromSeq < booking.getToStopSeq() && booking.getFromStopSeq() < toSeq) {
                bookedSeats.add(booking.getSeatNo());
            }
        }

        return bookedSeats;
    }

    private int calculateFare(String busType, int distanceKm) {
        FareRate fareRate = fareRateRepository.findById(busType).orElse(null);
        if (fareRate == null) {
            // Default rate if not found
            return distanceKm * 2; // ₹2 per km default
        }

        BigDecimal fare = fareRate.getRatePerKm().multiply(BigDecimal.valueOf(distanceKm));
        return fare.intValue();
    }

    private String determineSeatLayout(String seatNo, String busType) {
        // Simple layout logic - can be enhanced based on actual seat arrangements
        try {
            int seatNum = Integer.parseInt(seatNo);
            if (busType.contains("Sleeper")) {
                // Sleeper layout: typically 2+1 configuration
                int mod = seatNum % 3;
                if (mod == 1 || mod == 2) return "window";
                return "aisle";
            } else {
                // Seater layout: typically 2+2 configuration
                int mod = seatNum % 4;
                if (mod == 1 || mod == 0) return "window";
                return "aisle";
            }
        } catch (NumberFormatException e) {
            return "standard";
        }
    }
}