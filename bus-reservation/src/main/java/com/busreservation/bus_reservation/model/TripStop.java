package com.busreservation.bus_reservation.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "trip_stops")
public class TripStop {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tripStopId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(nullable = false)
    private Integer seqNo; // 0, 1, 2, ... (order in route)

    @Column(nullable = false)
    private String stopName;

    @Column
    private LocalTime arriveTime; // null for origin

    @Column
    private LocalTime departTime; // null for destination

    @Column(nullable = false)
    private Integer cumulativeKm; // distance from origin
}