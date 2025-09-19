package com.tiximax.txm.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tiximax.txm.Enums.WarehouseStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Getter
@Setter

public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(nullable = false)
    private String trackingCode;

    private Double length;

    private Double width;

    private Double height;

    private Double dim;

    private String image;

    @Column(nullable = false)
    private Double weight;

    @Column(nullable = false)
    private Double netWeight;

    @Enumerated(EnumType.STRING)
    private WarehouseStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

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
    @JoinColumn(name="packing_id", nullable = true)
    @JsonIgnore
    Packing packing;

    @ManyToOne
    @JoinColumn(name="purchase_id", nullable = false)
    @JsonIgnore
    Purchases purchase;

    @OneToMany(mappedBy = "warehouse", cascade = CascadeType.ALL)
    @JsonIgnore
    private Set<OrderLinks> orderLinks;

}
