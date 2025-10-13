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

public class Customer extends Account {

    @Column(unique = true, nullable = false)
    private String customerCode;

    private String address;

    private String source;

    private Long staffId;

    private Double totalWeight;

    private BigDecimal balance;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    @JsonIgnore
    Set<Orders> orders;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    @JsonIgnore
    Set<Payment> payments;

}
