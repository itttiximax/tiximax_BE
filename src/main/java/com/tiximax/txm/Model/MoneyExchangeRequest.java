package com.tiximax.txm.Model;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;
@Data
public class MoneyExchangeRequest {
     private Long destinationId;

    private BigDecimal exchangeRate;

    private BigDecimal fee;

    private Boolean checkRequired;

    private Long address; 

    private List<OrderLinkRequest> orderLinkRequests;
}
