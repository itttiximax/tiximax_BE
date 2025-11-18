package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.*;
import com.tiximax.txm.Enums.OrderLinkStatus;
import com.tiximax.txm.Enums.OrderStatus;
import com.tiximax.txm.Enums.PaymentStatus;
import com.tiximax.txm.Enums.PaymentType;
import com.tiximax.txm.Enums.ProcessLogAction;
import com.tiximax.txm.Enums.PurchaseFilter;
import com.tiximax.txm.Model.*;
import com.tiximax.txm.Repository.*;
import com.tiximax.txm.Utils.AccountUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private AccountRouteRepository accountRouteRepository;

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
        BigDecimal priceLinks = BigDecimal.ZERO;

        for (OrderLinks ol : orderLinks){
            priceLinks.add(ol.getTotalWeb());
        }

        if (purchaseRequest.getPurchaseTotal().compareTo(priceLinks) > 0){
            throw new IllegalStateException("Giá mua đang cao hơn giá tiền thu khách!");
        }

        if (!order.getStatus().equals(OrderStatus.CHO_MUA)){
            throw new RuntimeException("Đơn hàng chưa đủ điều kiện để mua hàng!");
        }

        boolean allBelongToOrder = orderLinks.stream()
                .allMatch(link -> link.getOrders().getOrderId().equals(order.getOrderId()));
        if (!allBelongToOrder) {
            throw new IllegalArgumentException("Tất cả mã phải thuộc cùng đơn hàng " + orderCode);
        }

        if(purchaseRequest.getShipmentCode() != ""){
            if (orderLinksRepository.existsByShipmentCode(purchaseRequest.getShipmentCode())) {
                throw new IllegalArgumentException("Một hoặc nhiều mã đã có mã vận đơn, không thể mua lại!");
            }
        }

        boolean allActive = orderLinks.stream()
                .allMatch(link -> link.getStatus() == OrderLinkStatus.CHO_MUA);
        if (!allActive) {
            throw new IllegalArgumentException("Tất cả mã phải ở trạng thái chờ mua!");
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
            orderLink.setStatus(OrderLinkStatus.DA_MUA);
            orderLink.setShipmentCode(purchaseRequest.getShipmentCode());
        }
        purchase.setOrderLinks(Set.copyOf(orderLinks));
        purchase = purchasesRepository.save(purchase);
        orderLinksRepository.saveAll(orderLinks);
        ordersService.addProcessLog(order, purchase.getPurchaseCode(), ProcessLogAction.DA_MUA_HANG);

        List<OrderLinks> allOrderLinks = orderLinksRepository.findByOrdersOrderId(order.getOrderId());

        long countcountDamua = allOrderLinks.stream()
                .filter(link -> link.getStatus() == OrderLinkStatus.DA_MUA)
                .count();
        long countCancel = allOrderLinks.stream()
                .filter(link -> link.getStatus() == OrderLinkStatus.DA_HUY)
                .count();
        if (countcountDamua > 0 && (countcountDamua + countCancel == allOrderLinks.size())) {
            order.setStatus(OrderStatus.CHO_NHAP_KHO_NN);
            ordersRepository.save(order);
}
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

        // Check if any orderLink has already been purchased
        boolean anyPurchased = orderLinks.stream()
                .anyMatch(link -> link.getPurchase() != null);
        if (anyPurchased) {
            throw new IllegalArgumentException("Một hoặc nhiều mã đã được mua, không thể mua lại!");
        }

        if (!order.getStatus().equals(OrderStatus.CHO_MUA)){
            throw new RuntimeException("Đơn hàng chưa đủ điều kiện để mua hàng!");
        }
        boolean allBelongToOrder = orderLinks.stream()
                .allMatch(link -> link.getOrders().getOrderId().equals(order.getOrderId()));
        if (!allBelongToOrder) {
            throw new IllegalArgumentException("Tất cả mã phải thuộc cùng đơn hàng " + orderCode);
        }
        //   if (orderLinksRepository.existsByShipmentCode(purchaseRequest.getShipmentCode())) {
        //     throw new IllegalArgumentException("Mã vận đơn đã tồn tại trong hệ thống, không thể xử lý!");
        // }

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
        purchasesRepository.save(purchase);
        orderLinksRepository.saveAll(orderLinks);
        ordersService.addProcessLog(order, purchase.getPurchaseCode(), ProcessLogAction.DAU_GIA_THANH_CONG);

        List<OrderLinks> allOrderLinks = orderLinksRepository.findByOrdersOrderId(order.getOrderId());
        
             long countcountDamua = allOrderLinks.stream()
                .filter(link -> link.getStatus() == OrderLinkStatus.DA_MUA)
                .count();
        long countCancel = allOrderLinks.stream()
                .filter(link -> link.getStatus() == OrderLinkStatus.DA_HUY)
                .count();
        if (countcountDamua > 0 && (countcountDamua + countCancel == allOrderLinks.size())) {
            order.setStatus(OrderStatus.CHO_NHAP_KHO_NN);
            ordersRepository.save(order);
}
   

        boolean allOrderLinksArePurchased = allOrderLinks.stream()
                .allMatch(link -> link.getStatus() == OrderLinkStatus.DA_MUA || link.getStatus() == OrderLinkStatus.DAU_GIA_THANH_CONG || link.getStatus() == OrderLinkStatus.DA_HUY
                );

        if (allOrderLinksArePurchased && !allOrderLinks.isEmpty()) {
            BigDecimal totalFinalPrice = purchasesRepository.getTotalFinalPriceByOrderId(order.getOrderId());


            if (totalFinalPrice.compareTo(order.getPriceBeforeFee()) > 0){
                Payment payment = new Payment();
                payment.setOrders(order);
                payment.setContent(order.getOrderCode());
                payment.setAmount(totalFinalPrice.subtract(order.getPriceBeforeFee()).multiply(order.getExchangeRate()));
                payment.setCollectedAmount(totalFinalPrice.subtract(order.getPriceBeforeFee()).multiply(order.getExchangeRate()));
                payment.setPaymentType(PaymentType.MA_QR);
                payment.setStatus(PaymentStatus.CHO_THANH_TOAN);
                payment.setPaymentCode(paymentService.generatePaymentCode());
                String qrCodeUrl = "https://img.vietqr.io/image/" + bankName + "-" + bankNumber + "-print.png?amount=" + totalFinalPrice.subtract(order.getPriceBeforeFee()).multiply(order.getExchangeRate()) + "&addInfo=" + payment.getPaymentCode() + "&accountName=" + bankOwner;
                payment.setActionAt(LocalDateTime.now());
                payment.setQrCode(qrCodeUrl);
                payment.setCustomer(order.getCustomer());
                payment.setStaff(order.getStaff());
                payment.setIsMergedPayment(false);
                paymentRepository.save(payment);
                order.setStatus(OrderStatus.CHO_THANH_TOAN_DAU_GIA);
            } else if(order.getPriceBeforeFee().compareTo(totalFinalPrice) > 0) {
                System.out.println("Số tiền cần thu: " + order.getPriceBeforeFee().subtract(totalFinalPrice).multiply(order.getExchangeRate()).negate());
                // Payment payment = new Payment();
                // payment.setOrders(order);
                // payment.setContent(order.getOrderCode());
                // payment.setAmount(order.getPriceBeforeFee().subtract(totalFinalPrice).multiply(order.getExchangeRate()).negate());
                // payment.setCollectedAmount(order.getPriceBeforeFee().subtract(totalFinalPrice).multiply(order.getExchangeRate()).negate());
                // payment.setStatus(PaymentStatus.CHO_THANH_TOAN);
                // payment.setPaymentCode(paymentService.generatePaymentCode());   
                // payment.setCustomer(order.getCustomer());
                // payment.setStaff(order.getStaff());
                // payment.setActionAt(LocalDateTime.now());
                // payment.setIsMergedPayment(false);
                // paymentRepository.save(payment);       
                order.setLeftoverMoney(order.getPriceBeforeFee().subtract(totalFinalPrice).multiply(order.getExchangeRate().negate()));
                order.setStatus(OrderStatus.CHO_NHAP_KHO_NN);      
            }
            else {
                order.setStatus(OrderStatus.CHO_NHAP_KHO_NN);
            }
                ordersRepository.save(order);
            }
         
        return purchase;
    }

    private String generatePurchaseCode() {
          String PurchaseCode;
        do {
            PurchaseCode = "MM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();;
        } while (purchasesRepository.existsByPurchaseCode(PurchaseCode));
        return PurchaseCode;
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

    public List<PendingShipmentPurchase> getPurchasesWithPendingShipment() {
        List<OrderLinks> pendingLinks = orderLinksRepository.findPendingShipmentLinks();

        return pendingLinks.stream()
                .filter(link -> link.getPurchase() != null)
                .collect(Collectors.groupingBy(OrderLinks::getPurchase))
                .entrySet().stream()
                .filter(entry -> entry.getKey() != null)
                .map(entry -> {
                    Purchases p = entry.getKey();
                    List<String> trackingCode = entry.getValue().stream()
                            .map(OrderLinks::getTrackingCode)
                            .toList();

                    PendingShipmentPurchase dto = new PendingShipmentPurchase();
                    dto.setPurchaseId(p.getPurchaseId());
                    dto.setPurchaseCode(p.getPurchaseCode());
                    dto.setOrderCode(p.getOrders().getOrderCode());
                    dto.setPurchaseTime(p.getPurchaseTime());
                    dto.setFinalPriceOrder(p.getFinalPriceOrder());
                    dto.setNote(p.getNote());
                    dto.setPurchaseImage(p.getPurchaseImage());
                    dto.setPendingTrackingCodes(trackingCode);
                    return dto;
                })
                .toList();
    }

    public Purchases updateShipmentForPurchase(Long purchaseId, String shipmentCode) {
         System.out.println("=== METHOD CALLED ===");
        if (shipmentCode == null || shipmentCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã vận đơn không được để trống!");
        }
        shipmentCode = shipmentCode.trim();

        System.out.println("Checking for existing shipment code: " + shipmentCode);

        if (orderLinksRepository.existsByShipmentCode(shipmentCode)) {
            throw new IllegalArgumentException("Mã vận đơn '" + shipmentCode + "' đã tồn tại!");
        }
        

        Purchases purchase = purchasesRepository.findById(purchaseId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch mua!"));

        for (OrderLinks link : purchase.getOrderLinks()) {
            link.setShipmentCode(shipmentCode);
        }

        orderLinksRepository.saveAll(purchase.getOrderLinks());

        return purchase;
    }

    public Purchases updateShipmentForPurchaseAndShipFee(Long purchaseId, String shipmentCode, BigDecimal shipFee) {
        if (shipmentCode == null || shipmentCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã vận đơn không được để trống!");
        }
        shipmentCode = shipmentCode.trim();

         System.out.println("Checking for existing shipment code: " + shipmentCode);

        if (orderLinksRepository.existsByShipmentCode(shipmentCode)) {
            throw new IllegalArgumentException("Mã vận đơn '" + shipmentCode + "' đã tồn tại!");
        }

        Purchases purchase = purchasesRepository.findById(purchaseId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch mua!"));

        var exchangeRate = purchase.getOrders().getExchangeRate();
        var fee = shipFee.multiply(exchangeRate);

        var order = purchase.getOrders();
        order.setLeftoverMoney(
        (order.getLeftoverMoney() == null ? BigDecimal.ZERO : order.getLeftoverMoney())
            .add(fee)
        );
        

        for (OrderLinks link : purchase.getOrderLinks()) {
            link.setShipmentCode(shipmentCode);
            link.setShipWeb(shipFee);
        }

        orderLinksRepository.saveAll(purchase.getOrderLinks());

        return purchase;
    }

    public Page<PurchasePendingShipment> getPendingShipmentPurchases(Pageable pageable) {
        Account currentAccount = accountUtils.getAccountCurrent();

        Set<Long> routeIds = accountRouteRepository.findByAccountAccountId(currentAccount.getAccountId())
                .stream()
                .map(AccountRoute::getRoute)
                .map(Route::getRouteId)
                .collect(Collectors.toSet());

        if (routeIds.isEmpty()) {
            return Page.empty(pageable);
        }

        Page<Purchases> purchasesPage = purchasesRepository.findPurchasesWithPendingShipmentByRoutes(routeIds, pageable);

        return purchasesPage.map(purchase -> {
            List<OrderLinkPending> pendingLinks = purchase.getOrderLinks().stream()
                    .filter(link -> link.getShipmentCode() == null || link.getShipmentCode().trim().isEmpty())
                    .map(OrderLinkPending::new)
                    .collect(Collectors.toList());

            return new PurchasePendingShipment(purchase, pendingLinks);
        });
    }

    

    public Page<PurchasePendingShipment> getFullPurchases(PurchaseFilter status ,Pageable pageable) {
        Account currentAccount = accountUtils.getAccountCurrent();
        Set<Long> routeIds = accountRouteRepository.findByAccountAccountId(currentAccount.getAccountId())
                .stream()
                .map(AccountRoute::getRoute)
                .map(Route::getRouteId)
                .collect(Collectors.toSet());
        if (routeIds.isEmpty()) {
            return Page.empty(pageable);
        }

         String statusValue = (status == null ? null : status.name());
        Page<Purchases> purchasesPage =
                purchasesRepository.findPurchasesSortedByPendingShipment(routeIds,statusValue, pageable);

        return purchasesPage.map(purchase -> {
            List<OrderLinkPending> pendingLinks = purchase.getOrderLinks().stream()
            //   .filter(link -> link.getShipmentCode() == null || link.getShipmentCode().trim().isEmpty())
                    .map(OrderLinkPending::new)
                    .collect(Collectors.toList());

            return new PurchasePendingShipment(purchase, pendingLinks);
        });
    }

    public Page<PurchasePendingShipment> getALLFullPurchases(PurchaseFilter status,Pageable pageable) {
        Account currentAccount = accountUtils.getAccountCurrent();

        Set<Long> routeIds = accountRouteRepository.findByAccountAccountId(currentAccount.getAccountId())
                .stream()
                .map(AccountRoute::getRoute)
                .map(Route::getRouteId)
                .collect(Collectors.toSet());

        if (routeIds.isEmpty()) {
            return Page.empty(pageable);
        }

        String statusValue = (status == null ? null : status.name());

        Page<Purchases> purchasesPage =
                purchasesRepository.findPurchasesWithFilteredOrderLinks(routeIds, statusValue,pageable);

        return purchasesPage.map(purchase -> {
            List<OrderLinkPending> pendingLinks = purchase.getOrderLinks().stream()
        //       .filter(link -> link.getShipmentCode() == null || link.getShipmentCode().trim().isEmpty())
                    .map(OrderLinkPending::new)
                    .collect(Collectors.toList());

            return new PurchasePendingShipment(purchase, pendingLinks);
        });
    }
}
