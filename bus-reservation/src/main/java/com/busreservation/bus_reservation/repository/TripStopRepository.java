package com.busreservation.bus_reservation.repository;

import com.busreservation.bus_reservation.model.TripStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TripStopRepository extends JpaRepository<TripStop, Long> {
    List<TripStop> findByTripIdOrderBySeqNoAsc(Long tripId);
    
    @Modifying
    @Query("DELETE FROM TripStop ts WHERE ts.trip.id = :tripId")
    void deleteByTripId(@Param("tripId") Long tripId);
}