package com.tiximax.txm.Model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Data
@Getter
@Setter

public class StaffPerformance {

    private String staffCode;

    private String name;

    private String department;

    private Long totalOrders;

    private BigDecimal totalGoods;

    private BigDecimal totalShip;

    private Long totalParcels;

    private Double completionRate;

    private Long badFeedbackCount;

    private Double totalNetWeight;

}