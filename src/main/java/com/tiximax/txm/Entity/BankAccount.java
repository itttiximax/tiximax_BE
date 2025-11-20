package com.tiximax.txm.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter

public class BankAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String bankName;

    @Column(nullable = false)
    private String accountHolder;

    @Column(nullable = false, unique = true)
    private String accountNumber;

    @Column(name = "is_proxy_payment", nullable = false)
    private Boolean isProxy = false;

    @Column(name = "is_revenue", nullable = false)
    private Boolean isRevenue = false;
}
