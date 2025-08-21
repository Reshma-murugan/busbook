package com.busreservation.bus_reservation.repository;

import com.busreservation.bus_reservation.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TripRepository extends JpaRepository<Trip, Long> {
    List<Trip> findByDayNoAndStatus(Integer dayNo, String status);
    
    @Query("SELECT t FROM Trip t WHERE t.dayNo = :dayNo AND t.bus.busId = :busId")
    Trip findByDayNoAndBusId(@Param("dayNo") Integer dayNo, @Param("busId") String busId);
}