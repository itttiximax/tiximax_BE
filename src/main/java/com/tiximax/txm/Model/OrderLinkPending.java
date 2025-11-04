package com.tiximax.txm.Model;

import com.tiximax.txm.Entity.OrderLinks;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter

public class OrderLinkPending {
    private Long linkId;
    private String productName;
    private Integer quantity;
    private String website;
    private String trackingCode;

    public OrderLinkPending(OrderLinks link) {
        this.linkId = link.getLinkId();
        this.productName = link.getProductName();
        this.quantity = link.getQuantity();
        this.website = link.getWebsite();
        this.trackingCode = link.getTrackingCode();
    }
}
