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
import com.tiximax.txm.Utils.AccountUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private PaymentRepository paymentRepository;

    @Autowired
    private PartialShipmentRepository partialShipmentRepository;

    public PartialShipment createPartialShipment(Long orderId, TrackingCodesRequest selectedTrackingCodes) {
        // kiểm tra đơn hàng tồn tại
        Orders order = ordersRepository.findById(orderId).orElseThrow();
        
        // kiểm tra các link đã đến kho VN
        List<OrderLinks> selectedLinks = orderLinksRepository.findByShipmentCodeIn(selectedTrackingCodes.getSelectedTrackingCodes()).stream()
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
        selectedLinks.forEach(link -> {
            link.setPartialShipment(partial);
            partial.getReadyLinks().add(link);
        });

        partialShipmentRepository.save(partial);
        orderLinksRepository.saveAll(selectedLinks);

        // Update Order status
        List<OrderLinks> allLinks = orderLinksRepository.findByOrdersOrderId(orderId);

        boolean allReady = allLinks.stream().filter(link -> link.getStatus() != OrderLinkStatus.DA_HUY)
            .allMatch(link ->link.getStatus() == OrderLinkStatus.DA_GIAO || link.getPartialShipment() != null );
        if (allReady) {
            order.setStatus(OrderStatus.CHO_THANH_TOAN_SHIP);
        } 
        ordersRepository.save(order);
        Payment partialPayment = new Payment();
        partialPayment.setAmount(partial.getPartialAmount());
        partialPayment.setPartialShipment(partial);
        partialPayment.setCollectedAmount(partial.getPartialAmount());
        partialPayment.setPaymentCode(paymentService.generatePaymentCode());
        partialPayment.setPaymentType(PaymentType.MA_QR);
        String qrCodeUrl = "https://img.vietqr.io/image/" + bankName + "-" + bankNumber + "-print.png?amount=" + partialPayment.getCollectedAmount() + "&addInfo=" + partialPayment.getPaymentCode() + "&accountName=" + bankOwner;
        partialPayment.setQrCode(qrCodeUrl);
        partialPayment.setActionAt(LocalDateTime.now());
        partialPayment.setActionAt(LocalDateTime.now());
        partialPayment.setCustomer(order.getCustomer());
        partialPayment.setStaff((Staff) accountUtils.getAccountCurrent());
        partialPayment.setOrders(order);
        partialPayment.setIsMergedPayment(false);
        paymentRepository.save(partialPayment);
        partial.setPayment(partialPayment);

        return partial;
    }
}
