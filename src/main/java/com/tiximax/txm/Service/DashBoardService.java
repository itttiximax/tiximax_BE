package com.tiximax.txm.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import com.tiximax.txm.Enums.PaymentStatus;
import com.tiximax.txm.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.tiximax.txm.Model.DashboardResponse;

@Service
public class DashBoardService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OrderLinksRepository orderLinksRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public DashboardResponse getDashboard(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();
        long totalOrders = ordersRepository.countByCreatedAtBetween(start, end);
        BigDecimal totalRevenue = paymentRepository.sumCollectedAmountBetween(start, end);
        BigDecimal totalPurchase = paymentRepository.sumPurchaseBetween(start, end);
        BigDecimal totalShip = paymentRepository.sumShipRevenueBetween(start, end);
        long newCustomers = customerRepository.countByCreatedAtBetween(start, end);
        long totalLinks = orderLinksRepository.countByOrdersCreatedAtBetween(start, end);


        DashboardResponse response = new DashboardResponse();
        response.setTotalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
        response.setTotalPurchase(totalPurchase != null ? totalPurchase : BigDecimal.ZERO);
        response.setTotalShip(totalShip != null ? totalShip : BigDecimal.ZERO);
        response.setTotalOrders(totalOrders);
        response.setNewCustomers(newCustomers);
        response.setTotalLinks(totalLinks);

        return response;
    }

    private LocalDateTime start() { return LocalDate.now().atStartOfDay(); }
    private LocalDateTime end()   { return LocalDate.now().plusDays(1).atStartOfDay(); }

    public Map<String, Long> getOrderCounts() {
        Map<String, Long> map = new HashMap<>();
        map.put("newOrders", ordersRepository.countByCreatedAtBetween(start(), end()));
        map.put("newOrderLinks", orderLinksRepository.countByOrders_CreatedAtBetween(start(), end()));
        return map;
    }

    public Map<String, Long> getCustomerCount() {
        Map<String, Long> map = new HashMap<>();
        map.put("newCustomers", customerRepository.countByCreatedAtBetween(start(), end()));
        return map;
    }

    public Map<String, BigDecimal> getPaymentSummary() {
        Map<String, BigDecimal> map = new HashMap<>();
        BigDecimal hang = paymentRepository.sumCollectedAmountByStatusAndActionAtBetween(
                PaymentStatus.DA_THANH_TOAN, start(), end());
        BigDecimal ship = paymentRepository.sumCollectedAmountByStatusAndActionAtBetween(
                PaymentStatus.DA_THANH_TOAN_SHIP, start(), end());

        map.put("totalCollectedAmount", hang != null ? hang : BigDecimal.ZERO);
        map.put("totalShipAmount", ship != null ? ship : BigDecimal.ZERO);
        return map;
    }

    public Map<String, Double> getWeightSummary() {
        Map<String, Double> map = new HashMap<>();
        Double w = warehouseRepository.sumNetWeightByCreatedAtBetween(start(), end());
        map.put("totalNetWeight", w != null ? w : 0.0);
        return map;
    }

}
