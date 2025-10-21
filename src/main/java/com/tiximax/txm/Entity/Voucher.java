package com.tiximax.txm.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tiximax.txm.Enums.AssignType;
import com.tiximax.txm.Enums.VoucherType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter

public class Voucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "voucherId")
    private Long voucherId;

    @Column(unique = true, nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VoucherType type;

    @Column(nullable = false)
    private BigDecimal value;

    private String description;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private BigDecimal minOrderValue;

    private Integer maxUses;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssignType assignType;

    private Double thresholdAmount;

    @OneToMany(mappedBy = "voucher", cascade = CascadeType.ALL)
    @JsonIgnore
    private Set<CustomerVoucher> customerVouchers;

    @ManyToMany
    @JoinTable(
            name = "voucher_route",
            joinColumns = @JoinColumn(name = "voucher_id"),
            inverseJoinColumns = @JoinColumn(name = "route_id")
    )
    private Set<Route> applicableRoutes = new HashSet<>();
}
