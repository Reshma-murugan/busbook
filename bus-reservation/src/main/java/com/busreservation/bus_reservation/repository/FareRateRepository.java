package com.busreservation.bus_reservation.repository;

import com.busreservation.bus_reservation.model.FareRate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FareRateRepository extends JpaRepository<FareRate, String> {
}