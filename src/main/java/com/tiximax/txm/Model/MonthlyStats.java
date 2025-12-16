package com.tiximax.txm.Model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MonthlyStats {
    private int month;
    private long totalOrders;
    private BigDecimal totalRevenue;
    private BigDecimal totalPurchase;
    private BigDecimal totalShip;
    private long newCustomers;
    private long totalLinks;
    private double totalWeight;

    public MonthlyStats(int month) {
        this.month = month;
        this.totalOrders = 0;
        this.totalRevenue = BigDecimal.ZERO;
        this.totalPurchase = BigDecimal.ZERO;
        this.totalShip = BigDecimal.ZERO;
        this.newCustomers = 0;
        this.totalLinks = 0;
        this.totalWeight = 0.0;
    }
}