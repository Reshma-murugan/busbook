package com.busreservation.bus_reservation.model;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "days_calendar")
public class DayCalendar {
    @Id
    private Integer dayNo; // 1-31
    
    @Column(nullable = false)
    private String label; // "Day 1", "Day 2", etc.
}