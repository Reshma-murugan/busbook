package com.busreservation.bus_reservation.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "bookings")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookingId;

    @Column(nullable = false, unique = true)
    private String pnr;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(nullable = false)
    private LocalDate bookingDate; // actual travel date

    @Column(nullable = false)
    private String seatNo;

    @Column(nullable = false)
    private Integer fromStopSeq;

    @Column(nullable = false)
    private Integer toStopSeq;

    @Column(nullable = false)
    private String passengerName;

    @Column(nullable = false)
    private String passengerPhone;

    @Column(nullable = false)
    private Integer fareAmount;

    @Column(nullable = false)
    private String status; // CONFIRMED, CANCELLED

    @Column(nullable = false)
    private LocalDateTime bookedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // optional for guest bookings
}