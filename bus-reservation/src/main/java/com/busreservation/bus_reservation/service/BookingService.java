package com.busreservation.bus_reservation.service;

import com.busreservation.bus_reservation.dto.BookingDto;
import com.busreservation.bus_reservation.exception.BadRequestException;
import com.busreservation.bus_reservation.exception.NotFoundException;
import com.busreservation.bus_reservation.model.*;
import com.busreservation.bus_reservation.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final TripRepository tripRepository;
    private final TripStopRepository tripStopRepository;
    private final FareRateRepository fareRateRepository;
    private final UserRepository userRepository;

    public BookingService(BookingRepository bookingRepository, TripRepository tripRepository,
                         TripStopRepository tripStopRepository, FareRateRepository fareRateRepository,
                         UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.tripRepository = tripRepository;
        this.tripStopRepository = tripStopRepository;
        this.fareRateRepository = fareRateRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public BookingDto.BookingResponse createBooking(String userEmail, BookingDto.BookingRequest request) {
        // Validate future date and time
        validateBookingDateTime(request.getDate(), request.getTripId(), request.getFromStopSeq());

        Trip trip = tripRepository.findById(request.getTripId())
                .orElseThrow(() -> new NotFoundException("Trip not found"));

        if (!"Running".equals(trip.getStatus())) {
            throw new BadRequestException("Trip is not available for booking (maintenance)");
        }

        // Get trip stops
        List<TripStop> stops = tripStopRepository.findByTripIdOrderBySeqNoAsc(request.getTripId());
        TripStop fromStop = stops.stream()
                .filter(s -> s.getSeqNo().equals(request.getFromStopSeq()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("From stop not found"));
        
        TripStop toStop = stops.stream()
                .filter(s -> s.getSeqNo().equals(request.getToStopSeq()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("To stop not found"));

        if (fromStop.getSeqNo() >= toStop.getSeqNo()) {
            throw new BadRequestException("Invalid journey segment");
        }

        // Check seat availability for the requested segment
        Set<String> bookedSeats = getBookedSeatsForSegment(
                request.getTripId(), request.getDate(), 
                request.getFromStopSeq(), request.getToStopSeq());

        if (bookedSeats.contains(request.getSeatNo())) {
            throw new BadRequestException("Seat " + request.getSeatNo() + " is not available");
        }

        // Calculate fare
        int distanceKm = toStop.getCumulativeKm() - fromStop.getCumulativeKm();
        int fareAmount = calculateFare(trip.getBus().getBusType(), distanceKm);

        // Get user (optional for guest bookings)
        User user = null;
        if (userEmail != null) {
            user = userRepository.findByEmail(userEmail).orElse(null);
        }

        // Create booking
        String pnr = generatePNR();
        Booking booking = Booking.builder()
                .pnr(pnr)
                .trip(trip)
                .bookingDate(request.getDate())
                .seatNo(request.getSeatNo())
                .fromStopSeq(request.getFromStopSeq())
                .toStopSeq(request.getToStopSeq())
                .passengerName(request.getPassenger().getName())
                .passengerPhone(request.getPassenger().getPhone())
                .fareAmount(fareAmount)
                .status("CONFIRMED")
                .bookedAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")))
                .user(user)
                .build();

        booking = bookingRepository.save(booking);

        // Build booking details
        BookingDto.BookingDetails details = BookingDto.BookingDetails.builder()
                .busId(trip.getBus().getBusId())
                .busType(trip.getBus().getBusType())
                .travelDate(request.getDate())
                .fromStop(fromStop.getStopName())
                .toStop(toStop.getStopName())
                .seatNo(request.getSeatNo())
                .passenger(request.getPassenger())
                .build();

        return BookingDto.BookingResponse.builder()
                .bookingId(booking.getBookingId())
                .pnr(booking.getPnr())
                .status(booking.getStatus())
                .fareAmount(booking.getFareAmount())
                .bookedAt(booking.getBookedAt())
                .details(details)
                .build();
    }

    public BookingDto.BookingResponse getBookingByPnr(String pnr) {
        Booking booking = bookingRepository.findByPnr(pnr)
                .orElseThrow(() -> new NotFoundException("Booking not found"));

        Trip trip = booking.getTrip();
        List<TripStop> stops = tripStopRepository.findByTripIdOrderBySeqNoAsc(trip.getId());
        
        TripStop fromStop = stops.stream()
                .filter(s -> s.getSeqNo().equals(booking.getFromStopSeq()))
                .findFirst().orElse(null);
        
        TripStop toStop = stops.stream()
                .filter(s -> s.getSeqNo().equals(booking.getToStopSeq()))
                .findFirst().orElse(null);

        BookingDto.BookingDetails details = BookingDto.BookingDetails.builder()
                .busId(trip.getBus().getBusId())
                .busType(trip.getBus().getBusType())
                .travelDate(booking.getBookingDate())
                .fromStop(fromStop != null ? fromStop.getStopName() : "Unknown")
                .toStop(toStop != null ? toStop.getStopName() : "Unknown")
                .seatNo(booking.getSeatNo())
                .passenger(BookingDto.PassengerInfo.builder()
                        .name(booking.getPassengerName())
                        .phone(booking.getPassengerPhone())
                        .build())
                .build();

        return BookingDto.BookingResponse.builder()
                .bookingId(booking.getBookingId())
                .pnr(booking.getPnr())
                .status(booking.getStatus())
                .fareAmount(booking.getFareAmount())
                .bookedAt(booking.getBookedAt())
                .details(details)
                .build();
    }

    private void validateBookingDateTime(LocalDate date, Long tripId, Integer fromStopSeq) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        if (date.isBefore(today)) {
            throw new BadRequestException("Cannot book for past dates");
        }

        // If booking for today, check departure time
        if (date.equals(today)) {
            List<TripStop> stops = tripStopRepository.findByTripIdOrderBySeqNoAsc(tripId);
            TripStop fromStop = stops.stream()
                    .filter(s -> s.getSeqNo().equals(fromStopSeq))
                    .findFirst().orElse(null);
            
            if (fromStop != null && fromStop.getDepartTime() != null) {
                LocalTime now = LocalTime.now(ZoneId.of("Asia/Kolkata"));
                if (fromStop.getDepartTime().isBefore(now)) {
                    throw new BadRequestException("Cannot book for past departure times");
                }
            }
        }
    }

    private Set<String> getBookedSeatsForSegment(Long tripId, LocalDate date, Integer fromSeq, Integer toSeq) {
        List<Booking> bookings = bookingRepository.findByTripIdAndBookingDateAndStatus(tripId, date, "CONFIRMED");
        Set<String> bookedSeats = new HashSet<>();

        if (requestFromSeq == null || requestToSeq == null) {
            return bookedSeats;

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
            return distanceKm * 2; // Default ₹2 per km
        }
        
        BigDecimal fare = fareRate.getRatePerKm().multiply(BigDecimal.valueOf(distanceKm));
        return fare.intValue();
    }

    private String generatePNR() {
        return "MGT" + System.currentTimeMillis() + String.format("%03d", new Random().nextInt(1000));
    }
}