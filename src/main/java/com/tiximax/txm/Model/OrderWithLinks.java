package com.tiximax.txm.Model;

import com.tiximax.txm.Entity.OrderLinks;
import com.tiximax.txm.Entity.Orders;
import com.tiximax.txm.Enums.OrderStatus;
import com.tiximax.txm.Enums.OrderType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
public class OrderWithLinks {
    private Long orderId;
    private String orderCode;
    private OrderType orderType;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private BigDecimal exchangeRate;
    private BigDecimal finalPriceOrder;
    private Boolean checkRequired;
    private String note;
    private Set<OrderLinks> orderLinks;

    public OrderWithLinks(Orders order) {
        this.orderId = order.getOrderId();
        this.orderCode = order.getOrderCode();
        this.orderType = order.getOrderType();
        this.status = order.getStatus();
        this.createdAt = order.getCreatedAt();
        this.exchangeRate = order.getExchangeRate();
        this.finalPriceOrder = order.getFinalPriceOrder();
        this.checkRequired = order.getCheckRequired();
        this.note = order.getNote();
        this.orderLinks = order.getOrderLinks();
    }
}