package com.tiximax.txm.API;

import com.tiximax.txm.Entity.Orders;
import com.tiximax.txm.Model.OrdersRequest;
import com.tiximax.txm.Service.OrdersService;
import com.tiximax.txm.Utils.AccountUtils;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
@RequestMapping("/orders")
@SecurityRequirement(name = "bearerAuth")

public class OrdersController {

    @Autowired
    private AccountUtils accountUtils;

    @Autowired
    private OrdersService ordersService;

    @PostMapping("/{customerCode}/{routeId}")
    public ResponseEntity<Orders> createdReview(@PathVariable String customerCode, @PathVariable long routeId, @RequestBody OrdersRequest ordersRequest) {
        Orders orders = ordersService.addOrder(customerCode, routeId, ordersRequest);
        return ResponseEntity.ok(orders);
    }

}
