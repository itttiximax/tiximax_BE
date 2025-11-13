package com.tiximax.txm.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tiximax.txm.Model.DashboardResponse;
import com.tiximax.txm.Repository.CustomerRepository;
import com.tiximax.txm.Repository.OrderLinksRepository;
import com.tiximax.txm.Repository.OrdersRepository;
import com.tiximax.txm.Repository.PaymentRepository;

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

}
