package com.tiximax.txm.API;

import com.tiximax.txm.Entity.OrderLinks;
import com.tiximax.txm.Entity.Orders;
import com.tiximax.txm.Enums.OrderDestination;
import com.tiximax.txm.Enums.OrderStatus;
import com.tiximax.txm.Enums.OrderType;
import com.tiximax.txm.Model.ConsignmentRequest;
import com.tiximax.txm.Model.OrderDetail;
import com.tiximax.txm.Model.OrderPayment;
import com.tiximax.txm.Model.OrderWithLinks;
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
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/orders")
@SecurityRequirement(name = "bearerAuth")

public class OrdersController {

    @Autowired
    private AccountUtils accountUtils;

    @Autowired
    private OrdersService ordersService;

    @PostMapping("/{customerCode}/{routeId}/{addressId}")
    public ResponseEntity<Orders> createdReview(@PathVariable String customerCode,
                                                @PathVariable long routeId,
                                                @PathVariable long addressId,
                                                @RequestBody OrdersRequest ordersRequest) throws IOException {
        Orders orders = ordersService.addOrder(customerCode, routeId, addressId,ordersRequest);
        return ResponseEntity.ok(orders);
    }

    @PostMapping("/deposit/{customerCode}/{routeId}/{addressId}")
    public ResponseEntity<Orders> createdConsignment(@PathVariable String customerCode,
                                                     @PathVariable long routeId,
                                                     @PathVariable long addressId,
                                                     @RequestBody ConsignmentRequest consignmentRequest) throws IOException {
        Orders orders = ordersService.addConsignment(customerCode, routeId, addressId,consignmentRequest);
        return ResponseEntity.ok(orders);
    }

    @PutMapping("order-link/cancel/{orderId}/{orderLinkId}")
    public ResponseEntity<Orders> CancelOrderLink(@PathVariable Long orderId, @PathVariable Long orderLinkId) {
        Orders orders = ordersService.updateStatusOrderLink(orderId, orderLinkId);  
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

    @GetMapping("/{page}/{size}")
    public ResponseEntity<Page<Orders>> getAllOrders(@PathVariable int page,@PathVariable int size) {
         Sort sort = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Orders> ordersPage = ordersService.getAllOrdersPaging(pageable);
        return ResponseEntity.ok(ordersPage);
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
    public ResponseEntity<Page<OrderPayment>> getOrdersForPayment(@PathVariable int page, @PathVariable int size, @PathVariable(required = false) OrderStatus status) {
        Sort sort = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<OrderPayment> ordersPage = ordersService.getOrdersForPayment(pageable, status);
        return ResponseEntity.ok(ordersPage);
    }

    @GetMapping("/detail/{orderId}")
    public ResponseEntity<OrderDetail> getOrderDetail(@PathVariable Long orderId) {
        OrderDetail orderDetail = ordersService.getOrderDetail(orderId);
        return ResponseEntity.ok(orderDetail);
    }

    @GetMapping("/with-links/{page}/{size}")
    public ResponseEntity<Page<OrderWithLinks>> getOrdersWithLinksForPurchaser(@PathVariable int page, @PathVariable int size, @RequestParam OrderType orderType) {
        Pageable pageable = PageRequest.of(page, size);
        Page<OrderWithLinks> ordersPage = ordersService.getOrdersWithLinksForPurchaser(pageable, orderType);
        return ResponseEntity.ok(ordersPage);
    }

    @GetMapping("/orderLink/{orderLinkId}")
    public ResponseEntity<OrderLinks> getOrderLinkById(@PathVariable Long orderLinkId) {
        OrderLinks orderLink = ordersService.getOrderLinkById(orderLinkId);
        return ResponseEntity.ok(orderLink);
    }

    @GetMapping("/statistics/for-payment")
    public ResponseEntity<Map<String, Long>> getOrderStatusStatistics() {
        Map<String, Long> statistics = ordersService.getOrderStatusStatistics();
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/orders/by-customer/{customerCode}")
    public ResponseEntity<List<OrderPayment>> getOrdersByCustomer(@PathVariable String customerCode) {
        List<OrderPayment> orders = ordersService.getOrdersByCustomerCode(customerCode);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/orders-shipping/by-customer/{customerCode}")
    public ResponseEntity<List<OrderPayment>> getOrdersShippingByCustomer(@PathVariable String customerCode) {
        List<OrderPayment> orders = ordersService.getOrdersShippingByCustomerCode(customerCode);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/partial-for-customer/{customerCode}")
    public ResponseEntity<List<OrderLinks>> getLinksByCustomer(@PathVariable String customerCode) {
        List<OrderLinks> links = ordersService.getLinksInWarehouseByCustomer(customerCode);
        return ResponseEntity.ok(links);
    }

    @PutMapping("/buy-later/{orderId}/links/{orderLinkId}")
    public ResponseEntity<Orders> updateOrderLinkToBuyLater(@PathVariable Long orderId, @PathVariable Long orderLinkId) {
        Orders updatedOrder = ordersService.updateOrderLinkToBuyLater(orderId, orderLinkId);
        return ResponseEntity.ok(updatedOrder);
    }

    @PutMapping("/pin/{orderId}")
    public ResponseEntity<Void> pinOrder(@PathVariable Long orderId, @RequestParam boolean pin) {
        ordersService.pinOrder(orderId, pin);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/ready-for-partial/{page}/{size}")
    public ResponseEntity<List<OrderPayment>> getReadyOrdersForPartial(@PathVariable int page, @PathVariable int size) {
        Sort sort = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        List<OrderPayment> readyOrders = ordersService.getReadyOrdersForPartial(pageable);
        return ResponseEntity.ok(readyOrders);
    }

    @GetMapping("/refund/{page}/{size}")
    public ResponseEntity<Page<Orders>> getOrdersWithNegativeLeftoverMoney(
            @PathVariable int page,
            @PathVariable int size) {
        Sort sort = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Orders> ordersPage = ordersService.getOrdersWithNegativeLeftoverMoney(pageable);
        return ResponseEntity.ok(ordersPage);
    }

    @PutMapping("/refund-confirm/{orderId}")
    public ResponseEntity<Orders> processNegativeLeftoverMoney(
            @PathVariable Long orderId,
            @RequestParam boolean refundToCustomer) {
        Orders updatedOrder = ordersService.processNegativeLeftoverMoney(orderId, refundToCustomer);
        return ResponseEntity.ok(updatedOrder);
    }

    @GetMapping("/buy-later/{page}/{size}")
    public ResponseEntity<Page<OrderWithLinks>> getBuyLaterOrders(
            @PathVariable int page,
            @PathVariable int size,
            @RequestParam OrderType orderType) {

        Pageable pageable = PageRequest.of(page, size);
        Page<OrderWithLinks> result = ordersService.getOrdersWithBuyLaterLinks(pageable, orderType);
        return ResponseEntity.ok(result);
    }
}
