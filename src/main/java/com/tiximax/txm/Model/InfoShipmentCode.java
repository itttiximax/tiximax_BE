package com.tiximax.txm.Model;

import com.tiximax.txm.Entity.Customer;
import com.tiximax.txm.Entity.Destination;
import com.tiximax.txm.Entity.Orders;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter

public class InfoShipmentCode {

    Orders orders;

    String destinationName;

}
