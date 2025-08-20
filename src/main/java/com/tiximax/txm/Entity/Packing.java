package com.tiximax.txm.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tiximax.txm.Enums.PackingDestination;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Getter
@Setter

public class Packing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "packing_id")
    private Long packingId;

    @Column(nullable = false)
    private String flightCode;

    @Enumerated(EnumType.STRING)
    private PackingDestination destination;

    @Column(nullable = false)
    private String trackingList;

    @Column(nullable = false)
    private LocalDateTime packedDate;

    @ManyToOne
    @JoinColumn(name="staff_id", nullable = false)
    @JsonIgnore
    Staff staff;

    @OneToMany(mappedBy = "packing", cascade = CascadeType.ALL)
    @JsonIgnore
    Set<Warehouse> warehouses;

    @OneToMany(mappedBy = "packing", cascade = CascadeType.ALL)
    @JsonIgnore
    Set<Domestic> domestics;

    @OneToOne
    @JoinColumn(name="order_id", nullable = false)
    @JsonIgnore
    Orders orders;

}
