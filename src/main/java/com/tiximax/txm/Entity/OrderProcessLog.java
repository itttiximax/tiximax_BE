package com.tiximax.txm.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tiximax.txm.Enums.ProcessLogAction;
import com.tiximax.txm.Enums.StaffPosition;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter

public class OrderProcessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Enumerated(EnumType.STRING)
    private StaffPosition roleAtTime;

    @Enumerated(EnumType.STRING)
    private ProcessLogAction action;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @ManyToOne
    @JoinColumn(name="staff_id", nullable = false)
    @JsonIgnore
    Staff staff;

    @ManyToOne
    @JoinColumn(name="order_id", nullable = false)
    @JsonIgnore
    Orders orders;
}
