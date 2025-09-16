package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.MergedPayment;
import com.tiximax.txm.Entity.Orders;
import com.tiximax.txm.Entity.Staff;
import com.tiximax.txm.Enums.OrderStatus;
import com.tiximax.txm.Enums.PaymentStatus;
import com.tiximax.txm.Enums.ProcessLogAction;
import com.tiximax.txm.Repository.MergedPaymentRepository;
import com.tiximax.txm.Repository.OrdersRepository;
import com.tiximax.txm.Utils.AccountUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service

public class MergedPaymentService {

    private final String bankName = "sacombank";
    private final String bankNumber = "070119787309";
    private final String bankOwner = "TRAN TAN PHAT";

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private MergedPaymentRepository mergedPaymentRepository;

    @Autowired
    private OrdersService ordersService;

    @Autowired
    private AccountUtils accountUtils;

    public MergedPayment createMergedPayment(List<String> orderCodes) {

        List<Orders> orders = ordersRepository.findByOrderCodeInWithMergedPayment(orderCodes);

        if (orders.isEmpty() || orders.size() != orderCodes.size()) {
            throw new IllegalArgumentException("Một hoặc nhiều mã đơn hàng không tồn tại!");
        }

        boolean allSameStatus = orders.stream()
                .allMatch(order -> order.getStatus() == OrderStatus.DA_XAC_NHAN);
        if (!allSameStatus) {
            throw new IllegalArgumentException("Tất cả đơn hàng phải ở cùng trạng thái để gộp!");
        }

        Set<Long> customerIds = orders.stream()
                .map(order -> order.getCustomer().getAccountId())
                .collect(Collectors.toSet());
        if (customerIds.size() != 1) {
            throw new IllegalArgumentException("Tất cả đơn hàng phải thuộc cùng một khách hàng để gộp!");
        }

        BigDecimal totalAmount = orders.stream()
                .map(Orders::getFinalPriceOrder)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        MergedPayment mergedPayment = new MergedPayment();
        mergedPayment.setPaymentCode(generatePaymentCode());
        mergedPayment.setTotalAmount(totalAmount);
        mergedPayment.setCollectedAmount(totalAmount);
        mergedPayment.setStatus(PaymentStatus.CHO_THANH_TOAN);
        String qrCodeUrl = "https://img.vietqr.io/image/" + bankName + "-" + bankNumber+ "-print.png?amount=" + mergedPayment.getCollectedAmount() + "&addInfo=" + mergedPayment.getPaymentCode() + "&accountName=" + bankOwner;
        mergedPayment.setQrCode(qrCodeUrl);
        mergedPayment.setActionAt(LocalDateTime.now());
        mergedPayment.setContent(mergedPayment.getPaymentCode());
        mergedPayment.setCustomer(orders.get(0).getCustomer());
        mergedPayment.setStaff((Staff) accountUtils.getAccountCurrent());
        mergedPayment.setOrders(new HashSet<>(orders));

        orders.forEach(order -> {
            order.setMergedPayment(mergedPayment);
            order.setStatus(OrderStatus.CHO_THANH_TOAN);
            ordersService.addProcessLog(order, mergedPayment.getPaymentCode(), ProcessLogAction.TAO_THANH_TOAN_HANG);
        });

        mergedPaymentRepository.save(mergedPayment);
        ordersRepository.saveAll(orders);

        return mergedPayment;
    }

    public String generatePaymentCode() {
        String paymentCode;
        do {
            paymentCode = "MG-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();;
        } while (mergedPaymentRepository.existsByPaymentCode(paymentCode));
        return paymentCode;
    }

}
