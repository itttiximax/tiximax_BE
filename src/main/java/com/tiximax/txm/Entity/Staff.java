package com.tiximax.txm.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tiximax.txm.Enums.StaffDepartment;
import com.tiximax.txm.Enums.StaffPosition;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Entity
@Getter
@Setter

public class Staff extends Account {

    @Column(unique = true, nullable = false)
    private String staffCode;

    @Enumerated(EnumType.STRING)
    private StaffDepartment department;

    @Enumerated(EnumType.STRING)
    private StaffPosition position;

    private String location;

    @OneToMany(mappedBy = "staff", cascade = CascadeType.ALL)
    @JsonIgnore
    Set<Warehouse> warehouses;

    @OneToMany(mappedBy = "staff", cascade = CascadeType.ALL)
    @JsonIgnore
    Set<Orders> orders;

    @OneToMany(mappedBy = "staff", cascade = CascadeType.ALL)
    @JsonIgnore
    Set<OrderProcessLog> orderProcessLogs;

    @OneToMany(mappedBy = "staff", cascade = CascadeType.ALL)
    @JsonIgnore
    Set<Packing> packings;

    @OneToMany(mappedBy = "staff", cascade = CascadeType.ALL)
    @JsonIgnore
    Set<Domestic> domestics;

    @OneToMany(mappedBy = "staff", cascade = CascadeType.ALL)
    @JsonIgnore
    Set<Purchases> purchases;

    @OneToMany(mappedBy = "staff", cascade = CascadeType.ALL)
    @JsonIgnore
    Set<Payment> payments;

}
