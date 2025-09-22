//package com.tiximax.txm.Entity;
//
//import com.fasterxml.jackson.annotation.JsonIgnore;
//import com.tiximax.txm.Enums.PaymentStatus;
//import jakarta.persistence.*;
//import lombok.Getter;
//import lombok.Setter;
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.util.Set;
//
//@Entity
//@Getter
//@Setter
//
//public class MergedPayment {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "merged_id")
//    private Long mergedId;
//
//    @Column(nullable = false, unique = true)
//    private String paymentCode;
//
//    @Column(nullable = false)
//    private BigDecimal totalAmount;
//
//    @Column(nullable = false)
//    private BigDecimal collectedAmount;
//
//    @Enumerated(EnumType.STRING)
//    private PaymentStatus status;
//
//    @Column(nullable = false)
//    private String qrCode;
//
//    @Column(nullable = false)
//    private LocalDateTime actionAt;
//
//    @Column
//    private String content;
//
//    @ManyToOne
//    @JoinColumn(name = "customer_id", nullable = false)
//    @JsonIgnore
//    Customer customer;
//
//    @ManyToOne
//    @JoinColumn(name = "staff_id", nullable = false)
//    @JsonIgnore
//    Staff staff;
//
//    @OneToMany(mappedBy = "mergedPayment", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
//    @JsonIgnore
//    Set<Orders> orders;
//
//}