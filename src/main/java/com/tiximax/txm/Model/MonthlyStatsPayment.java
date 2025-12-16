package com.tiximax.txm.Model;

import lombok.Data;

import java.math.BigDecimal;

@Data

public class MonthlyStatsPayment {
    private int month;
    private BigDecimal totalRevenue;
    private BigDecimal totalShip;

    public MonthlyStatsPayment(int month) {
        this.month = month;
        this.totalRevenue = BigDecimal.ZERO;
        this.totalShip = BigDecimal.ZERO;
    }
}
