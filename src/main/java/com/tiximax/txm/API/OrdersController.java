package com.tiximax.txm.API;

import com.tiximax.txm.Entity.Orders;
import com.tiximax.txm.Enums.OrderDestination;
import com.tiximax.txm.Enums.OrderStatus;
import com.tiximax.txm.Enums.OrderType;
import com.tiximax.txm.Model.OrderDetail;
import com.tiximax.txm.Model.OrdersRequest;
import com.tiximax.txm.Service.OrdersService;
import com.tiximax.txm.Utils.AccountUtils;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

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
    public ResponseEntity<Orders> createdReview(@PathVariable String customerCode, @PathVariable long routeId, @RequestBody OrdersRequest ordersRequest) throws IOException {
        Orders orders = ordersService.addOrder(customerCode, routeId, ordersRequest);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/enum-order-types")
    public ResponseEntity<List<String>> getOrderTypes() {
        List<String> orderTypes = Arrays.stream(OrderType.values())
                .map(Enum::name)
                .toList();
        return ResponseEntity.ok(orderTypes);
    }

    @GetMapping("/enum-order-destination")
    public ResponseEntity<List<String>> getOrderDestination() {
        List<String> orderDestination = Arrays.stream(OrderDestination.values())
                .map(Enum::name)
                .toList();
        return ResponseEntity.ok(orderDestination);
    }

    @GetMapping("/enum-order-type")
    public ResponseEntity<List<String>> getOrderType() {
        List<String> orderType = Arrays.stream(OrderType.values())
                .map(Enum::name)
                .toList();
        return ResponseEntity.ok(orderType);
    }

    @GetMapping
    public ResponseEntity<List<Orders>> getAllOrders() {
        List<Orders> orders = ordersService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{page}/{size}/{status}/paging")
    public ResponseEntity<Page<Orders>> getOrdersPaging(@PathVariable int page, int size, @PathVariable(required = false) OrderStatus status) {
        Sort sort = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Orders> ordersPage = ordersService.getOrdersPaging(pageable, status);
        return ResponseEntity.ok(ordersPage);
    }

    @GetMapping("/list-for-purchase")
    public ResponseEntity<List<Orders>> getOrdersForCurrentStaff() {
        List<Orders> orders = ordersService.getOrdersForCurrentStaff();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/for-payment/{page}/{size}/{status}")
    public ResponseEntity<Page<Orders>> getOrdersForPayment(@PathVariable int page, @PathVariable int size, @PathVariable(required = false) OrderStatus status) {
        Sort sort = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Orders> ordersPage = ordersService.getOrdersForPayment(pageable, status);
        return ResponseEntity.ok(ordersPage);
    }

    @GetMapping("/detail/{orderId}")
    public ResponseEntity<OrderDetail> getOrderDetail(@PathVariable Long orderId) {
        OrderDetail orderDetail = ordersService.getOrderDetail(orderId);
        return ResponseEntity.ok(orderDetail);
    }

}
