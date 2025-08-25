package com.tiximax.txm.Model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Data
@Getter
@Setter

public class RouteRequest {

    private String name;

    private String shipTime;

    private BigDecimal unitShippingPrice;

    private String note;

}
