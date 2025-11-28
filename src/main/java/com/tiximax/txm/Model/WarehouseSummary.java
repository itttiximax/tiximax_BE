package com.tiximax.txm.Model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter


public class WarehouseSummary {

    private Long warehouseId;
    private String trackingCode;
    private String orderCode;
    private Double weight;
    private Double netWeight;
    private Double dim;
    private String image;
    private String imageCheck;
    private LocalDateTime createdAt;

    public WarehouseSummary(Long warehouseId, String trackingCode, String orderCode, Double weight, Double netWeight, Double dim, String image, String imageCheck, LocalDateTime createdAt) {
        this.warehouseId = warehouseId;
        this.trackingCode = trackingCode;
        this.orderCode = orderCode;
        this.weight = weight;
        this.netWeight = netWeight;
        this.dim = dim;
        this.image = image;
        this.imageCheck = imageCheck;
        this.createdAt = createdAt;
    }

}
