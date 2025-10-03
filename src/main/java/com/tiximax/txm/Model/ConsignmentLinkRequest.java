package com.tiximax.txm.Model;

import java.math.BigDecimal;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter

public class ConsignmentLinkRequest {

    private Integer quantity;

    private String productName;
    
    private BigDecimal extraCharge;

    private String purchaseImage;

    private Long productTypeId;

    private String note;
}
