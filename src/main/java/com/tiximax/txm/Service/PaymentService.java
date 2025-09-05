package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.*;
import com.tiximax.txm.Enums.OrderStatus;
import com.tiximax.txm.Enums.PaymentStatus;
import com.tiximax.txm.Enums.PaymentType;
import com.tiximax.txm.Enums.ProcessLogAction;
import com.tiximax.txm.Repository.OrdersRepository;
import com.tiximax.txm.Repository.PaymentRepository;
import com.tiximax.txm.Repository.ProcessLogRepository;
import com.tiximax.txm.Repository.WarehouseRepository;
import com.tiximax.txm.Utils.AccountUtils;
import org.hibernate.query.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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
    private WarehouseService warehouseService;

    @Autowired
    private WarehouseRepository warehouseRepository;

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

    public Payment updatePayment(String paymentCode, Payment updatedPayment) {
        Optional<Payment> existingPayment = paymentRepository.findByPaymentCode(paymentCode);
        if (existingPayment.isPresent()) {
            Payment payment = existingPayment.get();
            payment.setPaymentCode(updatedPayment.getPaymentCode());
            payment.setContent(updatedPayment.getContent());
            payment.setPaymentType(updatedPayment.getPaymentType());
            payment.setAmount(updatedPayment.getAmount());
            payment.setCollectedAmount(updatedPayment.getCollectedAmount());
            payment.setStatus(updatedPayment.getStatus());
            payment.setQrCode(updatedPayment.getQrCode());
            payment.setActionAt(updatedPayment.getActionAt());
            payment.setCustomer(updatedPayment.getCustomer());
            payment.setStaff(updatedPayment.getStaff());
            payment.setOrders(updatedPayment.getOrders());
            return paymentRepository.save(payment);
        } else {
            throw new RuntimeException("Không tìm thấy giao dịch này!");
        }
    }

    public void deletePayment(String paymentCode) {
        if (paymentRepository.existsByPaymentCode(paymentCode)) {
            paymentRepository.deleteByPaymentCode(paymentCode);
        } else {
            throw new RuntimeException("Không tìm thấy giao dịch này!");
        }
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
        if (paymentOptional.isPresent()) {
            Payment payment = paymentOptional.get();
            if (paymentOptional.get().getStatus().equals(PaymentStatus.CHO_THANH_TOAN)){
                payment.setStatus(PaymentStatus.DA_THANH_TOAN);
                payment.setActionAt(LocalDateTime.now());
                Orders orders = payment.getOrders();
                orders.setStatus(OrderStatus.CHO_MUA);
                ordersRepository.save(orders);
                ordersService.addProcessLog(orders, payment.getPaymentCode(), ProcessLogAction.DA_THANH_TOAN);
                return paymentRepository.save(payment);
            } else {
                throw new RuntimeException("Trạng thái đơn hàng không phải chờ thanh toán!");
            }
        } else {
            throw new RuntimeException("Không tìm thấy giao dịch này!");
        }
    }

    public Payment createShippingPayment(String orderCode) {
        Orders orders = ordersRepository.findByOrderCode(orderCode);
        if (orders == null) {
            throw new RuntimeException("Không tìm thấy đơn hàng này!");
        }

        if (!orders.getStatus().equals(OrderStatus.CHO_THANH_TOAN_SHIP)){
            throw new RuntimeException("Đơn hàng chưa đủ điều kiện để thanh toán phí ship!");
        }

        Set<Purchases> purchases = orders.getPurchases();
        for (Purchases purchase : purchases) {
            if (!warehouseService.isPurchaseFullyReceived(purchase.getPurchaseId())) {
                throw new RuntimeException("Đơn hàng chưa đủ hàng trong kho để tạo thanh toán shipping!");
            }
        }

        List<Warehouse> warehouses = warehouseRepository.findByOrdersOrderCode(orderCode);
        double totalKg = 0;
        for (Warehouse warehouse : warehouses) {
            totalKg += warehouse.getWeight();
        }

        BigDecimal unitShippingPrice = orders.getRoute().getUnitShippingPrice();
        if (unitShippingPrice == null) {
            throw new RuntimeException("Không tìm thấy giá ship niêm yết cho tuyến này!");
        }

        BigDecimal shippingAmount = unitShippingPrice.multiply(BigDecimal.valueOf(totalKg));

        Payment payment = new Payment();
        payment.setPaymentCode(generatePaymentCode());
        payment.setContent("Thanh toán shipping cho đơn hàng: " + orders.getOrderCode());
        payment.setPaymentType(PaymentType.MA_QR);
        payment.setAmount(shippingAmount);
        payment.setCollectedAmount(shippingAmount);
        payment.setStatus(PaymentStatus.CHO_THANH_TOAN);
        String qrCodeUrl = "https://img.vietqr.io/image/" + bankName + "-" + bankNumber+ "-print.png?amount=" + shippingAmount + "&addInfo=" + payment.getPaymentCode() + "&accountName=" + bankOwner;
        payment.setQrCode(qrCodeUrl);
        payment.setActionAt(LocalDateTime.now());
        payment.setCustomer(orders.getCustomer());
        payment.setStaff((Staff) accountUtils.getAccountCurrent());
        payment.setOrders(orders);

        Payment savedPayment = paymentRepository.save(payment);
        ordersService.addProcessLog(orders, savedPayment.getPaymentCode(), ProcessLogAction.TAO_THANH_TOAN_SHIP);

        return savedPayment;
    }

}
