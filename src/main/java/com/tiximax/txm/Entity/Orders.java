package com.tiximax.txm.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tiximax.txm.Enums.OrderDestination;
import com.tiximax.txm.Enums.OrderStatus;
import com.tiximax.txm.Enums.OrderType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Getter
@Setter

public class Orders {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @Column(nullable = false)
    private String orderCode;

    @Enumerated(EnumType.STRING)
    private OrderType orderType;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private BigDecimal exchangeRate;

    private BigDecimal finalPriceOrder;

    @Column(nullable = false)
    private Boolean checkRequired;

    private String note;

    @ManyToOne
    @JoinColumn(name="customer_id", nullable = false)
//    @JsonIgnore
    Customer customer;

    @ManyToOne
    @JoinColumn(name="staff_id", nullable = false)
//    @JsonIgnore
    Staff staff;

    @OneToMany(mappedBy = "orders", cascade = CascadeType.ALL)
//    @JsonIgnore
    Set<Warehouse> warehouses;

    @OneToMany(mappedBy = "orders", cascade = CascadeType.ALL)
//    @JsonIgnore
    Set<Payment> payments;

    @OneToMany(mappedBy = "orders", cascade = CascadeType.ALL)
//    @JsonIgnore
    Set<Purchases> purchases;

    @OneToMany(mappedBy = "orders", cascade = CascadeType.ALL)
//    @JsonIgnore
    Set<Domestic> domestics;

    @OneToMany(mappedBy = "orders", cascade = CascadeType.ALL)
//    @JsonIgnore
    Set<OrderProcessLog> orderProcessLogs;

    @OneToMany(mappedBy = "orders", cascade = CascadeType.ALL)
//    @JsonIgnore
    Set<OrderLinks> orderLinks;

    @OneToMany(mappedBy = "orders", cascade = CascadeType.ALL)
//    @JsonIgnore
    Set<ShipmentTracking> shipmentTrackings;

    @ManyToOne
    @JoinColumn(name="route_id", nullable = false)
//    @JsonIgnore
    Route route;

    @ManyToOne
    @JoinColumn(name="destination_id", nullable = false)
//    @JsonIgnore
    Destination destination;

    @OneToOne(mappedBy = "orders", cascade = CascadeType.ALL)
//    @JsonIgnore
    Feedback feedback;

    @ManyToOne
    @JoinColumn(name = "packing_id", nullable = true)
//    @JsonIgnore
    Packing packing;
}
//package com.tiximax.txm.Entity;
//
//import com.tiximax.txm.Enums.OrderDestination;
//import com.tiximax.txm.Enums.OrderStatus;
//import com.tiximax.txm.Enums.OrderType;
//import jakarta.persistence.*;
//import lombok.Getter;
//import lombok.Setter;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.util.Set;
//
//@Entity
//@Getter
//@Setter
//public class Orders {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "order_id")
//    private Long orderId;
//
//    @Column(nullable = false)
//    private String orderCode;
//
//    @Enumerated(EnumType.STRING)
//    private OrderType orderType;
//
//    @Enumerated(EnumType.STRING)
//    private OrderStatus status;
//
//    @Column(nullable = false, updatable = false)
//    private LocalDateTime createdAt;
//
//    private BigDecimal exchangeRate;
//
//    private BigDecimal finalPriceOrder;
//
//    @Column(nullable = false)
//    private Boolean checkRequired;
//
//    private String note;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "customer_id", nullable = false)
//    private Customer customer;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "staff_id", nullable = false)
//    private Staff staff;
//
//    @OneToMany(mappedBy = "orders", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
//    private Set<Warehouse> warehouses;
//
//    @OneToMany(mappedBy = "orders", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
//    private Set<Payment> payments;
//
//    @OneToMany(mappedBy = "orders", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
//    private Set<Purchases> purchases;
//
//    @OneToMany(mappedBy = "orders", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
//    private Set<Domestic> domestics;
//
//    @OneToMany(mappedBy = "orders", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
//    private Set<OrderProcessLog> orderProcessLogs;
//
//    @OneToMany(mappedBy = "orders", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
//    private Set<OrderLinks> orderLinks;
//
//    @OneToMany(mappedBy = "orders", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
//    private Set<ShipmentTracking> shipmentTrackings;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "route_id", nullable = false)
//    private Route route;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "destination_id", nullable = false)
//    private Destination destination;
//
//    @OneToOne(mappedBy = "orders", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
//    private Feedback feedback;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "packing_id", nullable = true)
//    private Packing packing;
//}