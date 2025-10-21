package com.tiximax.txm.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    private BigDecimal priceBeforeFee;

    private BigDecimal leftoverMoney;

    @Column(nullable = false)
    private Boolean checkRequired;

    @ManyToOne
    @JoinColumn(name="customer_id", nullable = false)
    @JsonIgnore
    Customer customer;

    @ManyToOne
    @JoinColumn(name="staff_id", nullable = false)
    @JsonIgnore
    Staff staff;

    @OneToMany(mappedBy = "orders", cascade = CascadeType.ALL)
    @JsonIgnore
    Set<Warehouse> warehouses;

    @OneToMany(mappedBy = "orders", cascade = CascadeType.ALL)
    @JsonIgnore
    Set<Payment> payments;

    @OneToMany(mappedBy = "orders", cascade = CascadeType.ALL)
    @JsonIgnore
    Set<Purchases> purchases; 
    
//    @OneToMany(mappedBy = "orders", cascade = CascadeType.ALL)
//    @JsonIgnore
//    Set<Domestic> domestics;

    @OneToMany(mappedBy = "orders", cascade = CascadeType.ALL)
    @JsonIgnore
    Set<OrderProcessLog> orderProcessLogs;

    @OneToMany(mappedBy = "orders", cascade = CascadeType.ALL)
    @JsonIgnore
    Set<OrderLinks> orderLinks;

    @OneToMany(mappedBy = "orders", cascade = CascadeType.ALL)
    @JsonIgnore
    Set<ShipmentTracking> shipmentTrackings;

    @ManyToOne
    @JoinColumn(name="route_id", nullable = false)
    @JsonIgnore
    Route route;

    @ManyToOne
    @JoinColumn(name="destination_id", nullable = false)
    @JsonIgnore
    Destination destination;

    @OneToOne(mappedBy = "orders", cascade = CascadeType.ALL)
    @JsonIgnore
    Feedback feedback;

    @ManyToOne
    @JoinColumn(name = "voucherAppliedId")
    @JsonIgnore
    private CustomerVoucher voucherApplied;

//    @ManyToOne
//    @JoinColumn(name = "packing_id", nullable = true)
//    @JsonIgnore
//    Packing packing;

}
