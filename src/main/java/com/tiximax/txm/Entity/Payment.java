package com.tiximax.txm.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tiximax.txm.Enums.PaymentStatus;
import com.tiximax.txm.Enums.PaymentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter

public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @Column(nullable = false)
    private String paymentCode;

    private String content;

    @Enumerated(EnumType.STRING)
    private PaymentType paymentType;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private BigDecimal collectedAmount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(nullable = false)
    private String qrCode;

    @Column(nullable = false)
    private LocalDateTime actionAt;

    @ManyToOne
    @JoinColumn(name="customer_id", nullable = false)
    @JsonIgnore
    Customer customer;

    @ManyToOne
    @JoinColumn(name="staff_id", nullable = false)
    @JsonIgnore
    Staff staff;

    @ManyToOne
    @JoinColumn(name="order_id", nullable = false)
    @JsonIgnore
    Orders orders;

}
