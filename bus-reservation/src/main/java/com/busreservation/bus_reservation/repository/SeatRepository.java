package com.busreservation.bus_reservation.repository;

import com.busreservation.bus_reservation.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByBusId(String busId);
}