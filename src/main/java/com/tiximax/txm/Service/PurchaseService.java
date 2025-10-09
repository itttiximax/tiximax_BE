package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.OrderLinks;
import com.tiximax.txm.Entity.Orders;
import com.tiximax.txm.Entity.Payment;
import com.tiximax.txm.Entity.Purchases;
import com.tiximax.txm.Entity.Staff;
import com.tiximax.txm.Enums.OrderLinkStatus;
import com.tiximax.txm.Enums.OrderStatus;
import com.tiximax.txm.Enums.PaymentStatus;
import com.tiximax.txm.Enums.PaymentType;
import com.tiximax.txm.Enums.ProcessLogAction;
import com.tiximax.txm.Model.OrderPayment;
import com.tiximax.txm.Model.PurchaseDetail;
import com.tiximax.txm.Model.PurchaseRequest;
import com.tiximax.txm.Repository.OrderLinksRepository;
import com.tiximax.txm.Repository.OrdersRepository;
import com.tiximax.txm.Repository.PaymentRepository;
import com.tiximax.txm.Repository.PurchasesRepository;
import com.tiximax.txm.Utils.AccountUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service

public class PurchaseService {

    private final String bankName = "sacombank";
    private final String bankNumber = "070119787309";
    private final String bankOwner = "TRAN TAN PHAT";

    @Autowired
    private OrderLinksRepository orderLinksRepository;

    @Autowired
    private PurchasesRepository purchasesRepository;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentService paymentService;
    

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
      
        if (!orderLinksRepository.existsByShipmentCode(purchaseRequest.getShipmentCode())) {
            throw new IllegalArgumentException("Một hoặc nhiều mã đã có mã vận đơn, không thể mua lại!");
        }
        boolean allActive = orderLinks.stream()
                .allMatch(link -> link.getStatus() == OrderLinkStatus.CHO_MUA);
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
    
    
    public Purchases createAuction(String orderCode, PurchaseRequest purchaseRequest) {
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
                .allMatch(link -> link.getStatus() == OrderLinkStatus.CHO_MUA ||link.getStatus() == OrderLinkStatus.DAU_GIA_THANH_CONG) ;
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
        purchase.setFinalPriceOrder(purchaseRequest.getPurchaseTotal());
        for (OrderLinks orderLink : orderLinks) {
            orderLink.setPurchase(purchase);
            orderLink.setStatus(OrderLinkStatus.DAU_GIA_THANH_CONG);
            orderLink.setShipmentCode(purchaseRequest.getShipmentCode());
        }
        purchase.setOrderLinks(Set.copyOf(orderLinks));
        purchase = purchasesRepository.save(purchase);
        orderLinksRepository.saveAll(orderLinks);
        ordersService.addProcessLog(order, purchase.getPurchaseCode(), ProcessLogAction.DAU_GIA_THANH_CONG);

        List<OrderLinks> allOrderLinks = orderLinksRepository.findByOrdersOrderId(order.getOrderId());
        boolean allOrderLinksArePurchased = allOrderLinks.stream()
                .allMatch(link -> link.getStatus() == OrderLinkStatus.DA_MUA || link.getStatus() == OrderLinkStatus.DAU_GIA_THANH_CONG);

        if (allOrderLinksArePurchased && !allOrderLinks.isEmpty()) {
            BigDecimal totalFinalPrice = purchasesRepository.getTotalFinalPriceByOrderId(order.getOrderId());
            if (totalFinalPrice.compareTo(order.getPriceBeforeFee()) >= 0){
                Payment payment = new Payment();
                payment.setOrders(order);
                payment.setContent(order.getOrderCode());
                payment.setAmount(totalFinalPrice.subtract(order.getPriceBeforeFee()).multiply(order.getExchangeRate()));
                payment.setCollectedAmount(totalFinalPrice.subtract(order.getPriceBeforeFee()).multiply(order.getExchangeRate()));
                payment.setPaymentType(PaymentType.MA_QR);
                payment.setStatus(PaymentStatus.CHO_THANH_TOAN);
                String qrCodeUrl = "https://img.vietqr.io/image/" + bankName + "-" + bankNumber + "-print.png?amount=" + totalFinalPrice.subtract(order.getPriceBeforeFee()).multiply(order.getExchangeRate()) + "&addInfo=" + payment.getPaymentCode() + "&accountName=" + bankOwner;
                payment.setActionAt(LocalDateTime.now());
                payment.setQrCode(qrCodeUrl);
                payment.setPaymentCode(paymentService.generatePaymentCode());
                payment.setCustomer(order.getCustomer());
                payment.setStaff(order.getStaff());
                payment.setIsMergedPayment(false);
                paymentRepository.save(payment);
                order.setStatus(OrderStatus.CHO_THANH_TOAN_DAU_GIA);
            } else {
                order.setStatus(OrderStatus.CHO_NHAP_KHO_NN);
            }
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
