package com.tiximax.txm.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter

public class CustomerVoucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customerVoucherId")
    private Long customerVoucherId;

    @ManyToOne
    @JoinColumn(name = "customerId", nullable = false)
    @JsonIgnore
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "voucherId", nullable = false)
    private Voucher voucher;

    private LocalDateTime assignedDate;

    private LocalDateTime usedDate;

    private Integer usesRemaining = 1;

    private boolean isUsed = false;
}
    