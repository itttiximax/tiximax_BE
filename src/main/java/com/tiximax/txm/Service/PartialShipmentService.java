package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.*;
import com.tiximax.txm.Enums.OrderLinkStatus;
import com.tiximax.txm.Enums.OrderStatus;
import com.tiximax.txm.Enums.PaymentStatus;
import com.tiximax.txm.Enums.PaymentType;
import com.tiximax.txm.Model.TrackingCodesRequest;
import com.tiximax.txm.Repository.OrderLinksRepository;
import com.tiximax.txm.Repository.OrdersRepository;
import com.tiximax.txm.Repository.PartialShipmentRepository;
import com.tiximax.txm.Repository.PaymentRepository;
import com.tiximax.txm.Repository.RouteRepository;
import com.tiximax.txm.Repository.WarehouseRepository;
import com.tiximax.txm.Utils.AccountUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service

public class PartialShipmentService {

    @Value("${bank.name}")
    private String bankName;

    @Value("${bank.number}")
    private String bankNumber;

    @Value("${bank.owner}")
    private String bankOwner;

    @Autowired
    private OrdersRepository ordersRepository;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private OrderLinksRepository orderLinksRepository;

    @Autowired
    private AccountUtils accountUtils;

    @Autowired
    private WarehouseRepository warehousereRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PartialShipmentRepository partialShipmentRepository;


public List<PartialShipment> createPartialShipment(String customerCode) {
    List<OrderStatus> validStatuses = Arrays.asList(OrderStatus.DANG_XU_LY, OrderStatus.DA_DU_HANG);
    List<Orders> orders = ordersRepository.findByCustomerCustomerCodeAndStatusIn(customerCode, validStatuses);

    if (orders.isEmpty()) {
        throw new IllegalArgumentException("Không có đơn hàng nào đang ở trạng thái ĐANG_XU_LY hoặc ĐÃ_ĐỦ_HÀNG cho khách " + customerCode);
    }

    List<PartialShipment> createdPartials = new ArrayList<>();
    List<String> allTrackingCodes = new ArrayList<>();
    Staff currentStaff = (Staff) accountUtils.getAccountCurrent();

    // 🔹 Lặp qua tất cả các đơn hàng
    for (Orders order : orders) {

        List<OrderLinks> selectedLinks = orderLinksRepository
                .findByOrdersOrderId(order.getOrderId())
                .stream()
                .filter(link -> link.getStatus() == OrderLinkStatus.DA_NHAP_KHO_VN)
                .collect(Collectors.toList());

        if (selectedLinks.isEmpty()) continue;

        System.out.println("Creating Partial Shipment for Order ID: " + order.getOrderId() + 
                           " with " + selectedLinks.size() + " links.");

        // ✅ Thu thập tất cả tracking code cho việc tính phí sau này
        allTrackingCodes.addAll(
            selectedLinks.stream()
                .map(OrderLinks::getShipmentCode)
                .distinct()
                .collect(Collectors.toList())
        );

        // ✅ Tạo Partial Shipment cho từng đơn
        PartialShipment partial = new PartialShipment();
        partial.setOrders(order);
        partial.setReadyLinks(new HashSet<>(selectedLinks));
        partial.setPartialAmount(selectedLinks.stream()
                .map(OrderLinks::getFinalPriceVnd)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        partial.setShipmentDate(LocalDateTime.now());
        partial.setStatus(OrderStatus.CHO_THANH_TOAN_SHIP);
        partial.setStaff(currentStaff);

        // Gán quan hệ 2 chiều
        selectedLinks.forEach(link -> link.setPartialShipment(partial));

        partialShipmentRepository.save(partial);
        orderLinksRepository.saveAll(selectedLinks);

        // ✅ Cập nhật trạng thái đơn nếu tất cả link đã có partial
        boolean allReady = orderLinksRepository.findByOrdersOrderId(order.getOrderId())
                .stream()
                .filter(link -> link.getStatus() != OrderLinkStatus.DA_HUY)
                .allMatch(link -> link.getStatus() == OrderLinkStatus.DA_GIAO || link.getPartialShipment() != null);
        if (allReady) {
            order.setStatus(OrderStatus.CHO_THANH_TOAN_SHIP);
            ordersRepository.save(order);
        }

        createdPartials.add(partial);
    }

    if (createdPartials.isEmpty()) {
        throw new IllegalArgumentException("Không có link nào hợp lệ để tạo Partial Shipment cho khách " + customerCode);
    }

    Orders firstOrder = orders.get(0);
    BigDecimal shipFee = calculateTotalShippingFee(firstOrder.getRoute().getRouteId(), allTrackingCodes);

    // 💳 Tạo Payment chung (1 payment cho tất cả partial)
    Payment mergedPayment = new Payment();
    mergedPayment.setAmount(shipFee);
    mergedPayment.setCollectedAmount(shipFee);
    mergedPayment.setPaymentCode(paymentService.generatePaymentCode());
    mergedPayment.setPaymentType(PaymentType.MA_QR);

    String qrCodeUrl = "https://img.vietqr.io/image/"
            + bankName + "-" + bankNumber + "-print.png?amount=" + mergedPayment.getCollectedAmount()
            + "&addInfo=" + mergedPayment.getPaymentCode()
            + "&accountName=" + bankOwner;
    mergedPayment.setQrCode(qrCodeUrl);
    mergedPayment.setStatus(PaymentStatus.CHO_THANH_TOAN_SHIP);
    mergedPayment.setActionAt(LocalDateTime.now());
    mergedPayment.setCustomer(firstOrder.getCustomer());
    mergedPayment.setStaff(currentStaff);
    mergedPayment.setIsMergedPayment(false);

    // 🔹 Liên kết tất cả partial shipments vào payment này
    for (PartialShipment partial : createdPartials) {
        partial.setPayment(mergedPayment);
        mergedPayment.getPartialShipments().add(partial);
    }

    paymentRepository.save(mergedPayment);
    partialShipmentRepository.saveAll(createdPartials);
    return createdPartials;
}

    private BigDecimal calculateTotalShippingFee(Long routeId, List<String> selectedTrackingCodes) {
    List<Warehouse> warehouses = warehousereRepository.findByTrackingCodeIn(selectedTrackingCodes);

    System.out.println("Tracking codes: " + selectedTrackingCodes);
    System.out.println("Warehouses found: " + warehouses.size());
    if (warehouses.isEmpty()) {
        throw new IllegalArgumentException("Không tìm thấy kiện hàng tương ứng với tracking codes");
    }
    var Route = routeRepository.findById(routeId).orElseThrow(() -> new IllegalArgumentException("Route không tồn tại"));

    BigDecimal ratePerKg = Route.getUnitBuyingPrice();

    BigDecimal totalFee = warehouses.stream()
        .map(w -> {
            Double net = w.getNetWeight();
            if (net == null) {
                throw new IllegalArgumentException(
                    "Thiếu netWeight cho kiện " + w.getTrackingCode()
                );
            }
            return BigDecimal.valueOf(net).multiply(ratePerKg);
        })
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    return totalFee;
}

}
