package com.tiximax.txm.Model;

import com.tiximax.txm.Entity.Purchases;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;

@Data
@Getter
@Setter

public class CustomerBalanceAndOrders {

    private BigDecimal balance;

    private List<OrderPayment> orders;

    public CustomerBalanceAndOrders(BigDecimal balance, List<OrderPayment> orders) {
        this.balance = balance;
        this.orders = orders;
    }

}
