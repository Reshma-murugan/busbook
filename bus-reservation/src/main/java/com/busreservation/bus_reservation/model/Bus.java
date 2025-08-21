package com.busreservation.bus_reservation.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "buses")
public class Bus {
    @Id
    private String busId; // From Excel: B001, B002, etc.

    @Column(nullable = false)
    private String busType; // "Non-AC Seater", "AC Sleeper", etc.

    @Column(nullable = false)
    private Integer capacity;

    @Column
    private String homeDepot;

    @OneToMany(mappedBy = "bus", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Seat> seats = new ArrayList<>();
}
