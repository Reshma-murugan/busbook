package com.busreservation.bus_reservation.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "fare_rates")
public class FareRate {
    @Id
    private String category; // "Non-AC Seater", "AC Sleeper", etc.

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal ratePerKm;
}