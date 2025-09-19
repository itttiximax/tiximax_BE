package com.tiximax.txm.Model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter


public class WarehouseSummary {

    private Long warehouseId;
    private String trackingCode;
    private String orderCode;
    private Double weight;
    private Double netWeight;
    private Double dim;

    public WarehouseSummary(Long warehouseId, String trackingCode, String orderCode, Double weight, Double netWeight, Double dim) {
        this.warehouseId = warehouseId;
        this.trackingCode = trackingCode;
        this.orderCode = orderCode;
        this.weight = weight;
        this.netWeight = netWeight;
        this.dim = dim;
    }

}
