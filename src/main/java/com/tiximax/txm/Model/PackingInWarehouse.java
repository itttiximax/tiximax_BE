package com.tiximax.txm.Model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter

public class PackingInWarehouse {

    private String packingCode;

    private LocalDateTime packedDate;

    private Map<String, Integer> trackingCodeToProductCount;

}
