package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.*;
import com.tiximax.txm.Enums.OrderLinkStatus;
import com.tiximax.txm.Enums.OrderStatus;
import com.tiximax.txm.Repository.OrderLinksRepository;
import com.tiximax.txm.Repository.OrdersRepository;
import com.tiximax.txm.Repository.PartialShipmentRepository;
import com.tiximax.txm.Repository.PaymentRepository;
import com.tiximax.txm.Utils.AccountUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service

public class PartialShipmentService {

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private OrderLinksRepository orderLinksRepository;

    @Autowired
    private AccountUtils accountUtils;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PartialShipmentRepository partialShipmentRepository;

    public PartialShipment createPartialShipment(Long orderId, List<Long> selectedLinkIds) {
        Orders order = ordersRepository.findById(orderId).orElseThrow();
        List<OrderLinks> selectedLinks = orderLinksRepository.findAllById(selectedLinkIds).stream()
                .filter(link -> link.getOrders().getOrderId().equals(orderId) && link.getStatus() == OrderLinkStatus.DA_NHAP_KHO_VN)
                .collect(Collectors.toList());

        if (selectedLinks.isEmpty()) throw new IllegalArgumentException("Không có đơn nào được chọn!");

        PartialShipment partial = new PartialShipment();
        partial.setOrders(order);
        partial.setReadyLinks(new HashSet<>(selectedLinks));
        partial.setPartialAmount(selectedLinks.stream().map(OrderLinks::getFinalPriceVnd).reduce(BigDecimal.ZERO, BigDecimal::add));
        partial.setShipmentDate(LocalDateTime.now());
        partial.setStatus(OrderStatus.CHO_THANH_TOAN_SHIP);
        partial.setStaff((Staff) accountUtils.getAccountCurrent());

        selectedLinks.forEach(link -> link.setPartialShipment(partial));

        partialShipmentRepository.save(partial);
        orderLinksRepository.saveAll(selectedLinks);

        // Update Order status
        List<OrderLinks> allLinks = orderLinksRepository.findByOrdersOrderId(orderId);
        if (allLinks.stream().allMatch(link -> link.getPartialShipment() != null || link.getStatus() == OrderLinkStatus.DA_GIAO)) {
            order.setStatus(OrderStatus.DA_DU_HANG);
        } else {
            order.setStatus(OrderStatus.CHO_THANH_TOAN_SHIP);
        }
        ordersRepository.save(order);

        Payment partialPayment = new Payment();
        paymentRepository.save(partialPayment);
        partial.setPayment(partialPayment);

        return partial;
    }
}
