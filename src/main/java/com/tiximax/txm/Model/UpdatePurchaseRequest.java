package com.tiximax.txm.Model;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor      // cần để Jackson deserialize khi body rỗng
@AllArgsConstructor
@Getter
@Setter

public class UpdatePurchaseRequest {

    private BigDecimal finalPriceOrder;

    private String note;

    private String shipmentCode;

    private String imagePurchased;

}
