package com.tiximax.txm.Model;

import com.tiximax.txm.Enums.OrderType;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
@Data
@Getter
@Setter

public class ConsignmentRequest {
  
    private OrderType orderType;

    private Long destinationId;

    private Boolean checkRequired;

    private List<ConsignmentLinkRequest> consignmentLinkRequests;

}
