package com.tiximax.txm.Model;

import lombok.Data;

@Data
public class MonthlyStatsWarehouse {
    private int month;
    private double totalWeight;

    public MonthlyStatsWarehouse(int month) {
        this.month = month;
        this.totalWeight = 0.0;
    }
}
