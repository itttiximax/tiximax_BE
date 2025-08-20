package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.OrderLinks;
import com.tiximax.txm.Entity.Orders;
import com.tiximax.txm.Entity.Purchases;
import com.tiximax.txm.Entity.Staff;
import com.tiximax.txm.Enums.OrderLinkStatus;
import com.tiximax.txm.Repository.OrderLinksRepository;
import com.tiximax.txm.Repository.OrdersRepository;
import com.tiximax.txm.Repository.PurchasesRepository;
import com.tiximax.txm.Utils.AccountUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service

public class PurchaseService {

    @Autowired
    private OrderLinksRepository orderLinksRepository;

    @Autowired
    private PurchasesRepository purchasesRepository;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private AccountUtils accountUtils;

    public Purchases createPurchase(String orderCode, List<String> trackingCodes) {
        Orders order = ordersRepository.findByOrderCode(orderCode);

        List<OrderLinks> orderLinks = orderLinksRepository.findByTrackingCodeIn(trackingCodes);
        if (orderLinks.size() != trackingCodes.size()) {
            throw new IllegalArgumentException("Một hoặc nhiều mã không được tìm thấy!");
        }

        boolean allBelongToOrder = orderLinks.stream()
                .allMatch(link -> link.getOrders().getOrderId().equals(order.getOrderId()));
        if (!allBelongToOrder) {
            throw new IllegalArgumentException("Tất cả mã phải thuộc cùng đơn hàng " + orderCode);
        }

        boolean allActive = orderLinks.stream()
                .allMatch(link -> link.getStatus() == OrderLinkStatus.HOAT_DONG);
        if (!allActive) {
            throw new IllegalArgumentException("Tất cả mã phải ở trạng thái HOẠT ĐỘNG!");
        }

        Purchases purchase = new Purchases();
        purchase.setTrackingNumber(generatePurchaseTrackingNumber());
        purchase.setPurchaseTime(LocalDateTime.now());
        purchase.setStaff((Staff) accountUtils.getAccountCurrent());
        purchase.setOrders(order);
        purchase.setNote("Mua hàng cho các OrderLinks thuộc đơn hàng " + orderCode);

        for (OrderLinks orderLink : orderLinks) {
            orderLink.setPurchase(purchase);
            orderLink.setStatus(OrderLinkStatus.DA_MUA);
        }
        purchase.setOrderLinks(Set.copyOf(orderLinks));

        purchase = purchasesRepository.save(purchase);
        orderLinksRepository.saveAll(orderLinks);

        return purchase;
    }

    private String generatePurchaseTrackingNumber() {
        String trackingNumber;
        do {
            trackingNumber = "MM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        } while (purchasesRepository.existsByTrackingNumber(trackingNumber));
        return trackingNumber;
    }
}
