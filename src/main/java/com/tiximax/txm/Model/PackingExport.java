
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
    private String productName;
    private Double height ;
    private Double length ;
    private Double width ;
    private Double weight ;
    private Double dim ;
    private Double netWeight ;
    private String customerCode;
    private String  customerName ;
//    private String customerPhone;
//    private String Address;
    private String destination;
    private String staffName ;

}
