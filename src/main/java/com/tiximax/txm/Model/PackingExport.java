
package com.tiximax.txm.Model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@Data

public class PackingExport {

    private String packingCode;
    
    private String orderCode;

    private String trackingCode;

    private BigDecimal height ;
    private BigDecimal length ;
    private BigDecimal width ;
    private BigDecimal dim ;
    private BigDecimal netWeight ;
    private String customerCode;
    private String  customerName ;
    private String customerPhone;
    private String Address;
    private String staffName ;


}
