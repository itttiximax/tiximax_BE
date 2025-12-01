package com.tiximax.txm.Model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Data
@Getter
@Setter

public class UpdatePurchaseRequest {

    private BigDecimal finalPriceOrder;

    private String note;

    private String shipmentCode;

    private String imagePurchased;

}
