package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.OrderLinks;
import com.tiximax.txm.Entity.Orders;
import com.tiximax.txm.Entity.Purchases;
import com.tiximax.txm.Entity.Staff;
import com.tiximax.txm.Enums.OrderLinkStatus;
import com.tiximax.txm.Enums.OrderStatus;
import com.tiximax.txm.Enums.ProcessLogAction;
import com.tiximax.txm.Model.PurchaseDetail;
import com.tiximax.txm.Model.PurchaseRequest;
import com.tiximax.txm.Repository.OrderLinksRepository;
import com.tiximax.txm.Repository.OrdersRepository;
import com.tiximax.txm.Repository.PurchasesRepository;
import com.tiximax.txm.Utils.AccountUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private OrdersService ordersService;

    @Autowired
    private AccountUtils accountUtils;

    public Purchases createPurchase(String orderCode, PurchaseRequest purchaseRequest) {
        Orders order = ordersRepository.findByOrderCode(orderCode);

        if (order == null) {
            throw new IllegalArgumentException("Không tìm thấy đơn hàng!");
        }

        List<OrderLinks> orderLinks = orderLinksRepository.findByTrackingCodeIn(purchaseRequest.getTrackingCode());
        if (orderLinks.size() != purchaseRequest.getTrackingCode().size()) {
            throw new IllegalArgumentException("Một hoặc nhiều mã không được tìm thấy!");
        }

        if (!order.getStatus().equals(OrderStatus.CHO_MUA)){
            throw new RuntimeException("Đơn hàng chưa đủ điều kiện để mua hàng!");
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
        purchase.setPurchaseCode(generatePurchaseCode());
        purchase.setPurchaseTime(LocalDateTime.now());
        purchase.setStaff((Staff) accountUtils.getAccountCurrent());
        purchase.setOrders(order);
        purchase.setNote(purchaseRequest.getNote());
        purchase.setPurchaseImage(purchaseRequest.getImage());

        for (OrderLinks orderLink : orderLinks) {
            orderLink.setPurchase(purchase);
            orderLink.setStatus(OrderLinkStatus.DA_MUA);
            orderLink.setShipmentCode(purchaseRequest.getShipmentCode());
        }
        purchase.setOrderLinks(Set.copyOf(orderLinks));

        purchase = purchasesRepository.save(purchase);
        orderLinksRepository.saveAll(orderLinks);
        ordersService.addProcessLog(order, purchase.getPurchaseCode(), ProcessLogAction.DA_MUA_HANG);

        List<OrderLinks> allOrderLinks = orderLinksRepository.findByOrdersOrderId(order.getOrderId());
        boolean allOrderLinksArePurchased = allOrderLinks.stream()
                .allMatch(link -> link.getStatus() == OrderLinkStatus.DA_MUA);

        if (allOrderLinksArePurchased && !allOrderLinks.isEmpty()) {
            order.setStatus(OrderStatus.CHO_NHAP_KHO_NN);
            ordersRepository.save(order);
        }
        return purchase;
    }

    private String generatePurchaseCode() {
        String purchaseCode;
        do {
            purchaseCode = "MM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        } while (purchasesRepository.existsByPurchaseCode(purchaseCode));
        return purchaseCode;
    }

    public Page<Purchases> getAllPurchases(Pageable pageable) {
        return purchasesRepository.findAll(pageable);
    }

    public PurchaseDetail getPurchaseById(Long purchaseId) {
        Purchases purchases = purchasesRepository.findById(purchaseId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng này!"));
        return new PurchaseDetail(purchases);
    }

    public void deletePurchase(Long id) {
        Purchases purchase = purchasesRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch mua này!"));

        if (!purchase.getOrderLinks().isEmpty()) {

        }

        purchasesRepository.delete(purchase);
    }

}
