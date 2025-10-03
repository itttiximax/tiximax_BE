package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.*;
import com.tiximax.txm.Enums.*;
import com.tiximax.txm.Repository.OrdersRepository;
import com.tiximax.txm.Repository.PaymentRepository;
import com.tiximax.txm.Repository.ProcessLogRepository;
import com.tiximax.txm.Repository.WarehouseRepository;
import com.tiximax.txm.Utils.AccountUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service

public class PaymentService {

    private final String bankName = "sacombank";
    private final String bankNumber = "070119787309";
    private final String bankOwner = "TRAN TAN PHAT";

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private AccountUtils accountUtils;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private OrdersService ordersService;

    @Autowired
    private ProcessLogRepository processLogRepository;

    public Payment createPayment(String orderCode) {
        Orders orders = ordersRepository.findByOrderCode(orderCode);

        if (orders == null){
            throw new RuntimeException("Không tìm thấy đơn hàng này!");
        }

        if (!orders.getStatus().equals(OrderStatus.DA_XAC_NHAN)){
            throw new RuntimeException("Đơn hàng chưa đủ điều kiện để thanh toán!");
        }

        Payment payment = new Payment();
        payment.setPaymentCode(generatePaymentCode());
        payment.setContent(orders.getOrderCode());
        payment.setPaymentType(PaymentType.MA_QR);
        payment.setAmount(orders.getFinalPriceOrder());
        payment.setCollectedAmount(orders.getFinalPriceOrder());
        payment.setStatus(PaymentStatus.CHO_THANH_TOAN);
        String qrCodeUrl = "https://img.vietqr.io/image/" + bankName + "-" + bankNumber+ "-print.png?amount=" + payment.getCollectedAmount() + "&addInfo=" + payment.getPaymentCode() + "&accountName=" + bankOwner;
        payment.setQrCode(qrCodeUrl);
        payment.setActionAt(LocalDateTime.now());
        payment.setCustomer(orders.getCustomer());
        payment.setStaff((Staff) accountUtils.getAccountCurrent());
        payment.setOrders(orders);
        payment.setIsMergedPayment(false);
        ordersService.addProcessLog(orders, payment.getPaymentCode(), ProcessLogAction.TAO_THANH_TOAN_HANG);
        orders.setStatus(OrderStatus.CHO_THANH_TOAN);
        ordersRepository.save(orders);
        return paymentRepository.save(payment);
    }

    public List<Payment> getPaymentsByOrderCode(String orderCode) {
        List<Payment> payments = paymentRepository.findByOrdersOrderCode(orderCode);
        if (payments == null){
            throw new RuntimeException("Không tìm thấy đơn hàng này!");
        }
        return payments;
    }

    public Optional<Payment> getPaymentByCode(String paymentCode) {
        Optional<Payment> payment = paymentRepository.findByPaymentCode(paymentCode);
        if (payment.isEmpty()){
            throw new RuntimeException("Không tìm thấy giao dịch này!");
        }
        return paymentRepository.findByPaymentCode(paymentCode);
    }

    public String generatePaymentCode() {
        String paymentCode;
        do {
            paymentCode = "GD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();;
        } while (paymentRepository.existsByPaymentCode(paymentCode));
        return paymentCode;
    }

    public Payment confirmedPayment(String paymentCode) {
        Optional<Payment> paymentOptional = paymentRepository.findByPaymentCode(paymentCode);
        if (paymentOptional.isEmpty()) {
            throw new RuntimeException("Không tìm thấy giao dịch này!");
        }
        Payment payment = paymentOptional.get();
        if (!payment.getStatus().equals(PaymentStatus.CHO_THANH_TOAN)) {
            throw new RuntimeException("Trạng thái đơn hàng không phải chờ thanh toán!");
        }
        payment.setStatus(PaymentStatus.DA_THANH_TOAN);
        payment.setCollectedAmount(payment.getAmount());
        payment.setActionAt(LocalDateTime.now());
        if (payment.getIsMergedPayment()) {
            Set<Orders> orders = payment.getRelatedOrders();
            for (Orders order : orders) {
                order.setStatus(OrderStatus.CHO_MUA);
                ordersRepository.save(order);
                ordersService.addProcessLog(order, payment.getPaymentCode(), ProcessLogAction.DA_THANH_TOAN);
            }
        } else {
            Orders order = payment.getOrders();
            order.setStatus(OrderStatus.CHO_MUA);
            ordersRepository.save(order);
            ordersService.addProcessLog(order, payment.getPaymentCode(), ProcessLogAction.DA_THANH_TOAN);
        }
        return paymentRepository.save(payment);
    }

    public Payment createMergedPaymentShipping(Set<String> orderCodes) {
        List<Orders> ordersList = ordersRepository.findAllByOrderCodeIn(new ArrayList<>(orderCodes));
        if (ordersList.size() != orderCodes.size()) {
            throw new RuntimeException("Một hoặc một số đơn hàng không được tìm thấy!");
        }
        if (ordersList.stream().anyMatch(o -> !o.getStatus().equals(OrderStatus.DA_DU_HANG))) {
            throw new RuntimeException("Một hoặc một số đơn hàng chưa đủ điều kiện để thanh toán!");
        }

        BigDecimal totalWeight = ordersList.stream()
                .flatMap(order -> order.getWarehouses().stream())
                .filter(warehouse -> warehouse != null && warehouse.getWeight() != null)
                .map(Warehouse::getWeight)
                .map(BigDecimal::valueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAmount = totalWeight.multiply(totalWeight);

        Payment payment = new Payment();
        payment.setPaymentCode(generateMergedPaymentCode());
        payment.setContent(orderCodes + " ");
        payment.setPaymentType(PaymentType.MA_QR);
        payment.setAmount(totalAmount);
        payment.setCollectedAmount(totalAmount);
        payment.setStatus(PaymentStatus.CHO_THANH_TOAN_SHIP);
        String qrCodeUrl = "https://img.vietqr.io/image/" + bankName + "-" + bankNumber + "-print.png?amount=" + totalAmount + "&addInfo=" + payment.getPaymentCode() + "&accountName=" + bankOwner;
        payment.setQrCode(qrCodeUrl);
        payment.setActionAt(LocalDateTime.now());
        payment.setCustomer(ordersList.get(0).getCustomer());
        payment.setStaff((Staff) accountUtils.getAccountCurrent());
        payment.setIsMergedPayment(true);
        payment.setRelatedOrders(new HashSet<>(ordersList));
        payment.setOrders(null);
        Payment savedPayment = paymentRepository.save(payment);

        for (Orders order : ordersList) {
            ordersService.addProcessLog(order, savedPayment.getPaymentCode(), ProcessLogAction.TAO_THANH_TOAN_SHIP);
            order.setStatus(OrderStatus.CHO_THANH_TOAN_SHIP);
            ordersRepository.save(order);
        }

        return savedPayment;
    }

    public Payment confirmedPaymentShipment(String paymentCode) {
        Optional<Payment> paymentOptional = paymentRepository.findByPaymentCode(paymentCode);
        if (paymentOptional.isEmpty()) {
            throw new RuntimeException("Không tìm thấy giao dịch này!");
        }
        Payment payment = paymentOptional.get();
        if (!payment.getStatus().equals(PaymentStatus.CHO_THANH_TOAN_SHIP)) {
            throw new RuntimeException("Trạng thái đơn hàng không phải chờ thanh toán!");
        }
        payment.setStatus(PaymentStatus.DA_THANH_TOAN_SHIP);
        payment.setCollectedAmount(payment.getAmount());
        payment.setActionAt(LocalDateTime.now());
        if (payment.getIsMergedPayment()) {
            Set<Orders> orders = payment.getRelatedOrders();
            for (Orders order : orders) {
                order.setStatus(OrderStatus.CHO_GIAO);
                Set<OrderLinks> orderLinks = order.getOrderLinks();
                for (OrderLinks orderLink : orderLinks) {
                    orderLink.setStatus(OrderLinkStatus.CHO_GIAO);
                }
                ordersRepository.save(order);
                ordersService.addProcessLog(order, payment.getPaymentCode(), ProcessLogAction.DA_THANH_TOAN);
            }
        } else {
            Orders order = payment.getOrders();
            order.setStatus(OrderStatus.CHO_GIAO);
            Set<OrderLinks> orderLinks = order.getOrderLinks();
            for (OrderLinks orderLink : orderLinks) {
                orderLink.setStatus(OrderLinkStatus.CHO_GIAO);
            }
            ordersRepository.save(order);
            ordersService.addProcessLog(order, payment.getPaymentCode(), ProcessLogAction.DA_THANH_TOAN);
        }
        return paymentRepository.save(payment);
    }

    public Optional<Payment> getPaymentsById(Long paymentId) {
        return paymentRepository.findById(paymentId);
    }

    public Optional<Payment> getPendingPaymentByOrderId(Long orderId) {
        if (!ordersRepository.existsById(orderId)) {
            throw new RuntimeException("Không tìm thấy đơn hàng này!");
        }
        return paymentRepository.findFirstByOrdersOrderIdAndStatus(orderId, PaymentStatus.CHO_THANH_TOAN);
    }

    public Payment createMergedPayment(Set<String> orderCodes) {
        List<Orders> ordersList = ordersRepository.findAllByOrderCodeIn(new ArrayList<>(orderCodes));
        if (ordersList.size() != orderCodes.size()) {
            throw new RuntimeException("Một hoặc một số đơn hàng không được tìm thấy!");
        }
        if (ordersList.stream().anyMatch(o -> !o.getStatus().equals(OrderStatus.DA_XAC_NHAN))) {
            throw new RuntimeException("Một hoặc một số đơn hàng chưa đủ điều kiện để thanh toán!");
        }
        BigDecimal totalAmount = ordersList.stream()
                .map(Orders::getFinalPriceOrder)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Payment payment = new Payment();
        payment.setPaymentCode(generateMergedPaymentCode());
        payment.setContent(orderCodes + " ");
        payment.setPaymentType(PaymentType.MA_QR);
        payment.setAmount(totalAmount);
        payment.setCollectedAmount(totalAmount);
        payment.setStatus(PaymentStatus.CHO_THANH_TOAN);
        String qrCodeUrl = "https://img.vietqr.io/image/" + bankName + "-" + bankNumber + "-print.png?amount=" + totalAmount + "&addInfo=" + payment.getPaymentCode() + "&accountName=" + bankOwner;
        payment.setQrCode(qrCodeUrl);
        payment.setActionAt(LocalDateTime.now());
        payment.setCustomer(ordersList.get(0).getCustomer());
        payment.setStaff((Staff) accountUtils.getAccountCurrent());
        payment.setIsMergedPayment(true);
        payment.setRelatedOrders(new HashSet<>(ordersList));
        payment.setOrders(null);
        Payment savedPayment = paymentRepository.save(payment);

        for (Orders order : ordersList) {
            ordersService.addProcessLog(order, savedPayment.getPaymentCode(), ProcessLogAction.TAO_THANH_TOAN_HANG);
            order.setStatus(OrderStatus.CHO_THANH_TOAN);
            ordersRepository.save(order);
        }

        return savedPayment;
    }

    public String generateMergedPaymentCode() {
        String paymentCode;
        do {
            paymentCode = "MG-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();;
        } while (paymentRepository.existsByPaymentCode(paymentCode));
        return paymentCode;
    }
}
