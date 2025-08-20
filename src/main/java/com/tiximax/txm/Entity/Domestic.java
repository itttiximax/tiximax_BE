package com.tiximax.txm.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tiximax.txm.Enums.DomesticFrom;
import com.tiximax.txm.Enums.DomesticTo;
import com.tiximax.txm.Enums.DomesticType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter

public class Domestic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "domestic_id")
    private Long domesticId;

    @Enumerated(EnumType.STRING)
    private DomesticFrom fromLocation;

    @Enumerated(EnumType.STRING)
    private DomesticTo toLocation;

    @Enumerated(EnumType.STRING)
    private DomesticType type;

    private String deliveryAddress;

    private String note;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @ManyToOne
    @JoinColumn(name="staff_id", nullable = false)
    @JsonIgnore
    Staff staff;

    @ManyToOne
    @JoinColumn(name="location_id", nullable = false)
    @JsonIgnore
    WarehouseLocation location;

    @ManyToOne
    @JoinColumn(name="order_id", nullable = false)
    @JsonIgnore
    Orders orders;

    @ManyToOne
    @JoinColumn(name="packing_id", nullable = false)
    @JsonIgnore
    Packing packing;

}
