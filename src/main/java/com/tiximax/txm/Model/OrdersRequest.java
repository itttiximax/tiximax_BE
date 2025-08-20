package com.tiximax.txm.Model;

import com.tiximax.txm.Entity.Route;
import com.tiximax.txm.Enums.OrderDestination;
import com.tiximax.txm.Enums.OrderType;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;

@Data
@Getter
@Setter

public class OrdersRequest {

    private OrderType orderType;

    private OrderDestination destination;

    private BigDecimal exchangeRate;

    private Boolean checkRequired;

    private String note;

    private List<OrderLinkRequest> orderLinkRequests;

}
