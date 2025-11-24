package com.tiximax.txm.Model;

import com.tiximax.txm.Entity.Customer;
import com.tiximax.txm.Entity.Orders;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Sort;

@Data
@Getter
@Setter

public class InfoShipmentCode {

    Orders orders;

    Customer customer;

}
