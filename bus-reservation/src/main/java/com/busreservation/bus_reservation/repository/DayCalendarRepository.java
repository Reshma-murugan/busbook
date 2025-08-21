package com.busreservation.bus_reservation.repository;

import com.busreservation.bus_reservation.model.DayCalendar;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DayCalendarRepository extends JpaRepository<DayCalendar, Integer> {
}