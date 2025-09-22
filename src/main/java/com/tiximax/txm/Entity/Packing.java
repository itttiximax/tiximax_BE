package com.tiximax.txm.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tiximax.txm.Enums.PackingDestination;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter

public class Packing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "packing_id")
    private Long packingId;

    @Column(nullable = true)
    private String flightCode;

    @Column(nullable = false)
    private String packingCode;

    @Column(nullable = false)
    private List<String> packingList = new ArrayList<>();

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

    @OneToMany(mappedBy = "packing", cascade = CascadeType.ALL)
    @JsonIgnore
    Set<Orders> orders;

    @ManyToOne
    @JoinColumn(name="destination_id", nullable = false)
    @JsonIgnore
    Destination destination;

}
