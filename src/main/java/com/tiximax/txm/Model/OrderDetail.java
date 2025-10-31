package com.tiximax.txm.Model;

import com.tiximax.txm.Entity.*;
import com.tiximax.txm.Enums.OrderStatus;
import com.tiximax.txm.Enums.OrderType;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Getter
@Setter

public class OrderDetail {

    private Long orderId;
    private String orderCode;
    private OrderType orderType;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private BigDecimal exchangeRate;
    private BigDecimal finalPriceOrder;
    private Boolean checkRequired;
    private String note;
    private Customer customer;
    private Staff staff;
    private Set<Warehouse> warehouses;
    private Set<Payment> payments;
    private Set<Purchases> purchases;
    private Set<OrderProcessLog> orderProcessLogs;
    private Set<OrderLinks> orderLinks;
    private Set<ShipmentTracking> shipmentTrackings;
    private Route route;
    private Destination destination;
    private Feedback feedback;

    public OrderDetail(Orders order) {
        this.orderId = order.getOrderId();
        this.orderCode = order.getOrderCode();
        this.orderType = order.getOrderType();
        this.status = order.getStatus();
        this.createdAt = order.getCreatedAt();
        this.exchangeRate = order.getExchangeRate();
        this.finalPriceOrder = order.getFinalPriceOrder();
        this.checkRequired = order.getCheckRequired();
        this.customer = order.getCustomer();
        this.staff = order.getStaff();
        this.warehouses = order.getWarehouses();
        this.payments = order.getPayments();
        this.purchases = order.getPurchases();
        this.orderProcessLogs = order.getOrderProcessLogs();
        this.orderLinks = order.getOrderLinks();
        this.shipmentTrackings = order.getShipmentTrackings();
        this.route = order.getRoute();
        this.destination = order.getDestination();
        this.feedback = order.getFeedback();
    }
}