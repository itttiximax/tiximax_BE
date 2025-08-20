package com.tiximax.txm.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tiximax.txm.Enums.CustomerType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Entity
@Getter
@Setter

public class Customer extends Account {

    @Column(unique = true, nullable = false)
    private String customerCode;

    @Enumerated(EnumType.STRING)
    private CustomerType type;

    private String address;

    private String taxCode;

    private String source;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    @JsonIgnore
    Set<Orders> orders;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    @JsonIgnore
    Set<Payment> payments;

}
