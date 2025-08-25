package com.tiximax.txm.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Set;

@Entity
@Getter
@Setter

public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "route_id")
    private Long routeId;

    private String name;

    private String shipTime;

    private BigDecimal unitShippingPrice;

    private BigDecimal unitPurchasingPrice;

    private String note;

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL)
    @JsonIgnore
    Set<Orders> orders;

}
