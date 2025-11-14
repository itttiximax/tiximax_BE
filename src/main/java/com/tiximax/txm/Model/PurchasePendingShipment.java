package com.tiximax.txm.Model;

import com.tiximax.txm.Entity.Purchases;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Getter
@Setter

public class PurchasePendingShipment {
    private Long purchaseId;
    private String purchaseCode;
    private LocalDateTime purchaseTime;
    private String orderCode;
    private Long orderId;
    private String staffName;
    private List<OrderLinkPending> pendingLinks;

    public PurchasePendingShipment(Purchases purchase, List<OrderLinkPending> links) {
        this.purchaseId = purchase.getPurchaseId();
        this.purchaseCode = purchase.getPurchaseCode();
        this.purchaseTime = purchase.getPurchaseTime();
        this.orderCode = purchase.getOrders().getOrderCode();
        this.orderId = purchase.getOrders().getOrderId();
        this.staffName = purchase.getStaff().getName();
        this.pendingLinks = links;
    }
}
