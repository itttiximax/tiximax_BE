package com.tiximax.txm.Model;

import com.tiximax.txm.Enums.OrderLinkProductType;
import com.tiximax.txm.Enums.OrderLinkStatus;
import com.tiximax.txm.Enums.OrderLinkWebsite;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Data
@Getter
@Setter

public class OrderLinkRequest {

    private String productLink;

    private Integer quantity;

    private BigDecimal priceWeb;

    private BigDecimal shipWeb;

    private String productName;
//    private BigDecimal totalWeb;

    private BigDecimal purchaseFee;

//    private BigDecimal finalPriceVnd;

    private String purchaseImage;

    private OrderLinkWebsite website;

    private OrderLinkProductType productType;

//    private OrderLinkStatus status;

    private String groupTag;

}
