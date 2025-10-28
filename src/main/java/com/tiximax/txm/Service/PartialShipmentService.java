package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.*;
import com.tiximax.txm.Enums.OrderLinkStatus;
import com.tiximax.txm.Enums.OrderStatus;
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
    List<Orders> orders = ordersRepository.findByCustomerCodeAndStatus(customerCode, OrderStatus.DANG_XU_LY);
    if (orders.isEmpty()) {
        throw new IllegalArgumentException("Không có đơn hàng nào đang ở trạng thái ĐANG_XU_LY cho khách " + customerCode);
    }

    List<PartialShipment> createdPartials = new ArrayList<>();

    for (Orders order : orders) {

        List<OrderLinks> selectedLinks = orderLinksRepository
                .findByOrdersOrderId(order.getOrderId())
                .stream()
                .filter(link -> link.getStatus() == OrderLinkStatus.DA_NHAP_KHO_VN)
                .collect(Collectors.toList());

        if (selectedLinks.isEmpty()) continue;

        PartialShipment partial = new PartialShipment();
        partial.setOrders(order);
        partial.setReadyLinks(new HashSet<>(selectedLinks));
        partial.setPartialAmount(selectedLinks.stream()
                .map(OrderLinks::getFinalPriceVnd)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        partial.setShipmentDate(LocalDateTime.now());
        partial.setStatus(OrderStatus.CHO_THANH_TOAN_SHIP);
        partial.setStaff((Staff) accountUtils.getAccountCurrent());

        selectedLinks.forEach(link -> link.setPartialShipment(partial));

        List<String> trackingCodes = selectedLinks.stream()
        .map(OrderLinks::getTrackingCode)
        .collect(Collectors.toList());

        BigDecimal shipFee = calculateTotalShippingFee(order.getRoute().getRouteId(), trackingCodes);

        partialShipmentRepository.save(partial);
        orderLinksRepository.saveAll(selectedLinks);

        boolean allReady = orderLinksRepository.findByOrdersOrderId(order.getOrderId())
                .stream()
                .filter(link -> link.getStatus() != OrderLinkStatus.DA_HUY)
                .allMatch(link -> link.getStatus() == OrderLinkStatus.DA_GIAO || link.getPartialShipment() != null);
        if (allReady) {
            order.setStatus(OrderStatus.CHO_THANH_TOAN_SHIP);
            ordersRepository.save(order);
        }

        // 💳 Tạo Payment (1 payment cho nhiều partial)
        Payment partialPayment = new Payment();
        partialPayment.setAmount(shipFee);
        partialPayment.setCollectedAmount(shipFee);
        partialPayment.setPaymentCode(paymentService.generatePaymentCode());
        partialPayment.setPaymentType(PaymentType.MA_QR);

        String qrCodeUrl = "https://img.vietqr.io/image/"
                + bankName + "-" + bankNumber + "-print.png?amount=" + partialPayment.getCollectedAmount()
                + "&addInfo=" + partialPayment.getPaymentCode()
                + "&accountName=" + bankOwner;
        partialPayment.setQrCode(qrCodeUrl);

        partialPayment.setActionAt(LocalDateTime.now());
        partialPayment.setCustomer(order.getCustomer());
        partialPayment.setStaff((Staff) accountUtils.getAccountCurrent());
        partialPayment.setOrders(order);
        partialPayment.setIsMergedPayment(true); 

        // 🔹 Liên kết 2 chiều
        partialPayment.getPartialShipments().add(partial);
        partial.setPayment(partialPayment);

        paymentRepository.save(partialPayment);
        partialShipmentRepository.save(partial);

        createdPartials.add(partial);
    }

    if (createdPartials.isEmpty()) {
        throw new IllegalArgumentException("Không có link nào hợp lệ để tạo Partial Shipment cho khách " + customerCode);
    }

    return createdPartials;
}

    private BigDecimal calculateTotalShippingFee(Long routeId, List<String> selectedTrackingCodes) {
    List<Warehouse> warehouses = warehousereRepository.findByTrackingCodeIn(selectedTrackingCodes);

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
