package com.tiximax.txm.Model;

import com.tiximax.txm.Entity.*;
import com.tiximax.txm.Enums.OrderStatus;
import com.tiximax.txm.Enums.OrderType;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Getter
@Setter

public class OrderPayment {
    private Long orderId;
    private String orderCode;
    private OrderType orderType;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private BigDecimal exchangeRate;
    private BigDecimal finalPriceOrder;
    private String note;
    private Customer customer;
    private String paymentCode;
    private BigDecimal totalNetWeight;

    public OrderPayment(Orders order) {
        this.orderId = order.getOrderId();
        this.orderCode = order.getOrderCode();
        this.orderType = order.getOrderType();
        this.status = order.getStatus();
        this.createdAt = order.getCreatedAt();
        this.exchangeRate = order.getExchangeRate();
        this.finalPriceOrder = order.getFinalPriceOrder();
        this.customer = order.getCustomer();
        this.paymentCode = null;
        this.totalNetWeight = order.getWarehouses() != null
                ? order.getWarehouses().stream()
                .map(Warehouse::getNetWeight)
                .filter(netWeight -> netWeight != null)
                .map(BigDecimal::valueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                : BigDecimal.ZERO;
    }

}