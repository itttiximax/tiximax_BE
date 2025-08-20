package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.OrderProcessLog;
import com.tiximax.txm.Entity.Orders;
import com.tiximax.txm.Entity.Payment;
import com.tiximax.txm.Entity.Staff;
import com.tiximax.txm.Enums.PaymentStatus;
import com.tiximax.txm.Enums.PaymentType;
import com.tiximax.txm.Enums.ProcessLogAction;
import com.tiximax.txm.Repository.OrdersRepository;
import com.tiximax.txm.Repository.PaymentRepository;
import com.tiximax.txm.Repository.ProcessLogRepository;
import com.tiximax.txm.Utils.AccountUtils;
import org.hibernate.query.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service

public class PaymentService {

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

        Payment payment = new Payment();
        payment.setPaymentCode(generatePaymentCode());
        payment.setContent(orders.getOrderCode());
        payment.setPaymentType(PaymentType.MA_QR);
        payment.setAmount(orders.getFinalPriceOrder());
        payment.setCollectedAmount(orders.getFinalPriceOrder());
        payment.setStatus(PaymentStatus.CHO_THANH_TOAN);
        payment.setQrCode("Mã QR");
        payment.setActionAt(LocalDateTime.now());
        payment.setCustomer(orders.getCustomer());
        payment.setStaff((Staff) accountUtils.getAccountCurrent());
        payment.setOrders(orders);
        ordersService.addProcessLog(orders, ProcessLogAction.TAO_THANH_TOAN);
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

//    public void deletePayment(String paymentCode) {
//        if (paymentRepository.existsByPaymentCode(paymentCode)) {
//            paymentRepository.deleteByPaymentCode(paymentCode);
//        } else {
//            throw new RuntimeException("Không tìm thấy giao dịch này!");
//        }
//    }

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
                ordersService.addProcessLog(orders, ProcessLogAction.DA_THANH_TOAN);
                return paymentRepository.save(payment);
            } else {
                throw new RuntimeException("Trạng thái đơn hàng không phải chờ thanh toán!");
            }
        } else {
            throw new RuntimeException("Không tìm thấy giao dịch này!");
        }
    }

}
