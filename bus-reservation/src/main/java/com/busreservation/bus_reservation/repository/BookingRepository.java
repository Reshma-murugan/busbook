package com.busreservation.bus_reservation.repository;

import com.busreservation.bus_reservation.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByTripIdAndBookingDateAndStatus(Long tripId, LocalDate bookingDate, String status);
    Optional<Booking> findByPnr(String pnr);
    List<Booking> findByUserIdOrderByBookedAtDesc(Long userId);
}