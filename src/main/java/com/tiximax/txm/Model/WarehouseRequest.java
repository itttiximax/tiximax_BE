package com.tiximax.txm.Model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter

public class WarehouseRequest {

    private Double length;

    private Double width;

    private Double height;

    private Double weight;

    private Double netWeight;

}
