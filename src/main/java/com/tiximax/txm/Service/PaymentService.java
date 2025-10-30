package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.*;
import com.tiximax.txm.Enums.*;
import com.tiximax.txm.Repository.*;
import com.tiximax.txm.Utils.AccountUtils;
import org.hibernate.query.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service

public class PaymentService {

    @Value("${bank.name}")
    private String bankName;

    @Value("${bank.number}")
    private String bankNumber;

    @Value("${bank.owner}")
    private String bankOwner;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PartialShipmentRepository partialShipmentRepository;

    @Autowired
    private AccountUtils accountUtils;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private OrdersService ordersService;

    @Autowired
    private OrderLinksRepository orderLinksRepository;

    @Autowired
    private ProcessLogRepository processLogRepository;

    @Autowired
    private AuthenticationRepository authenticationRepository;

    @Autowired
    private CustomerVoucherRepository customerVoucherRepository;

    public Payment createPayment(String orderCode, Integer depositPercent, boolean isUseBalance) {
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
        payment.setDepositPercent(depositPercent);
        if (isUseBalance){
            BigDecimal collect = orders.getFinalPriceOrder().multiply(BigDecimal.valueOf(depositPercent / 100.00)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal balance = orders.getCustomer().getBalance();
            if (balance.compareTo(collect) >= 0){
                orders.setLeftoverMoney(collect.subtract(payment.getAmount()).setScale(2, RoundingMode.HALF_UP));
                payment.setCollectedAmount(BigDecimal.ZERO);
                orders.getCustomer().setBalance(balance.subtract(collect));
            } else {
                orders.setLeftoverMoney(collect.subtract(payment.getAmount()).setScale(2, RoundingMode.HALF_UP));
                payment.setCollectedAmount(collect.subtract(balance));
                orders.getCustomer().setBalance(BigDecimal.ZERO);
            }
        } else {
            payment.setCollectedAmount(orders.getFinalPriceOrder().multiply(BigDecimal.valueOf(depositPercent / 100.00)).setScale(2, RoundingMode.HALF_UP));
        }
        payment.setStatus(PaymentStatus.CHO_THANH_TOAN);
        String qrCodeUrl = "https://img.vietqr.io/image/" + bankName + "-" + bankNumber + "-print.png?amount=" + payment.getCollectedAmount() + "&addInfo=" + payment.getPaymentCode() + "&accountName=" + bankOwner;
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
            if(order.getStatus() == OrderStatus.CHO_THANH_TOAN_DAU_GIA){
               order.setStatus(OrderStatus.CHO_NHAP_KHO_NN);
            } else
            order.setStatus(OrderStatus.CHO_MUA);
            ordersRepository.save(order);
            ordersService.addProcessLog(order, payment.getPaymentCode(), ProcessLogAction.DA_THANH_TOAN);
        }
        return paymentRepository.save(payment);
    }

//    public Payment createMergedPaymentShipping(Set<String> orderCodes, boolean isUseBalance) {
//        List<Orders> ordersList = ordersRepository.findAllByOrderCodeIn(new ArrayList<>(orderCodes));
//        if (ordersList.size() != orderCodes.size()) {
//            throw new RuntimeException("Một hoặc một số đơn hàng không được tìm thấy!");
//        }
//        if (ordersList.stream().anyMatch(o -> !o.getStatus().equals(OrderStatus.DA_DU_HANG))) {
//            throw new RuntimeException("Một hoặc một số đơn hàng chưa đủ điều kiện để thanh toán!");
//        }
//        BigDecimal unitPrice = null ;
//        if (ordersList.get(0).getOrderType() == OrderType.DAU_GIA) {
//            unitPrice = ordersList.get(0).getRoute().getUnitDepositPrice();
//        } else{
//            unitPrice = ordersList.get(0).getRoute().getUnitBuyingPrice();
//        }
//        unitPrice = ordersList.get(0).getRoute().getUnitBuyingPrice();
//        boolean hasNullNetWeight = ordersList.stream()
//                .flatMap(order -> order.getWarehouses().stream())
//                .anyMatch(warehouse -> warehouse != null && warehouse.getNetWeight() == null);
//        if (hasNullNetWeight) {
//            throw new RuntimeException("Một hoặc nhiều đơn hàng chưa được cân, vui lòng kiểm tra lại!");
//        }
//
//        BigDecimal totalWeight = ordersList.stream()
//                .flatMap(order -> order.getWarehouses().stream())
//                .filter(warehouse -> warehouse != null && warehouse.getWeight() != null)
//                .map(Warehouse::getWeight)
//                .map(BigDecimal::valueOf)
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        BigDecimal totalAmount = totalWeight.multiply(unitPrice);
//
//        Payment payment = new Payment();
//        payment.setPaymentCode(generateMergedPaymentCode());
//        payment.setContent(orderCodes + " ");
//        payment.setPaymentType(PaymentType.MA_QR);
//        payment.setAmount(totalAmount);
//
//        BigDecimal totalLeftover = ordersList.stream()
//                .map(Orders::getLeftoverMoney)
//                .filter(Objects::nonNull)
//                .reduce(BigDecimal.ZERO, BigDecimal::add)
//                .setScale(2, RoundingMode.HALF_UP);
//
//        if (isUseBalance) {
//            BigDecimal collect = totalAmount;
//            if (totalLeftover.compareTo(BigDecimal.ZERO) < 0) {
//                collect = collect.add(totalLeftover.abs()).setScale(2, RoundingMode.HALF_UP);
//            }
//            collect = collect.max(BigDecimal.ZERO);
//
//            BigDecimal balance = ordersList.get(0).getCustomer().getBalance();
//
//            if (balance.compareTo(collect) >= 0) {
//                BigDecimal leftoverValue = collect.subtract(totalAmount).setScale(2, RoundingMode.HALF_UP);
//                ordersList.forEach(order -> order.setLeftoverMoney(leftoverValue));
//                if (leftoverValue.compareTo(BigDecimal.ZERO) < 0) {
//                    payment.setCollectedAmount(leftoverValue.abs());
//                } else {
//                    payment.setCollectedAmount(BigDecimal.ZERO);
//                }
//                ordersList.get(0).getCustomer().setBalance(balance.subtract(collect).setScale(2, RoundingMode.HALF_UP));
//            } else {
//                BigDecimal leftoverValue = collect.subtract(totalAmount).setScale(2, RoundingMode.HALF_UP);
//                ordersList.forEach(order -> order.setLeftoverMoney(leftoverValue));
//                BigDecimal remaining = collect.subtract(balance).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
//                if (leftoverValue.compareTo(BigDecimal.ZERO) < 0) {
//                    payment.setCollectedAmount(remaining.add(leftoverValue.abs()).setScale(2, RoundingMode.HALF_UP));
//                } else {
//                    payment.setCollectedAmount(remaining);
//                }
//                ordersList.get(0).getCustomer().setBalance(BigDecimal.ZERO);
//            }
//        } else {
//            BigDecimal collect = totalAmount;
//            if (totalLeftover.compareTo(BigDecimal.ZERO) < 0) {
//                collect = collect.add(totalLeftover.abs()).setScale(2, RoundingMode.HALF_UP);
//            }
//            collect = collect.max(BigDecimal.ZERO);
//
//            BigDecimal leftoverValue = collect.subtract(totalAmount).setScale(2, RoundingMode.HALF_UP);
//            ordersList.forEach(order -> order.setLeftoverMoney(leftoverValue));
//            if (leftoverValue.compareTo(BigDecimal.ZERO) < 0) {
//                payment.setCollectedAmount(collect.add(leftoverValue.abs()).setScale(2, RoundingMode.HALF_UP));
//            } else {
//                payment.setCollectedAmount(collect);
//            }
//        }
//
//        payment.setStatus(PaymentStatus.CHO_THANH_TOAN_SHIP);
//        String qrCodeUrl = "https://img.vietqr.io/image/" + bankName + "-" + bankNumber + "-print.png?amount=" + totalAmount + "&addInfo=" + payment.getPaymentCode() + "&accountName=" + bankOwner;
//        payment.setQrCode(qrCodeUrl);
//        payment.setActionAt(LocalDateTime.now());
//        payment.setCustomer(ordersList.get(0).getCustomer());
//        payment.setStaff((Staff) accountUtils.getAccountCurrent());
//        payment.setIsMergedPayment(true);
//        payment.setRelatedOrders(new HashSet<>(ordersList));
//        payment.setOrders(null);
//        Payment savedPayment = paymentRepository.save(payment);
//
//        return savedPayment;
//    }

    public Payment createMergedPaymentShipping(Set<String> orderCodes, boolean isUseBalance, Long customerVoucherId) {
        if (orderCodes == null || orderCodes.isEmpty()) {
            throw new RuntimeException("Không tìm thấy đơn hàng nào!");
        }

        List<Orders> ordersList = ordersRepository.findAllByOrderCodeIn(new ArrayList<>(orderCodes));
        if (ordersList.size() != orderCodes.size()) {
            throw new RuntimeException("Một hoặc một số đơn hàng không được tìm thấy!");
        }
        if (ordersList.stream().anyMatch(o -> !o.getStatus().equals(OrderStatus.DA_DU_HANG))) {
            throw new RuntimeException("Một hoặc một số đơn hàng chưa đủ điều kiện để thanh toán!");
        }

        Customer commonCustomer = ordersList.get(0).getCustomer();
        if (ordersList.stream().anyMatch(o -> !o.getCustomer().equals(commonCustomer))) {
            throw new RuntimeException("Các đơn hàng phải thuộc cùng một khách hàng để thanh toán gộp!");
        }

        BigDecimal unitPrice;
        if (ordersList.get(0).getOrderType() == OrderType.DAU_GIA) {
            unitPrice = ordersList.get(0).getRoute().getUnitDepositPrice();
        } else {
            unitPrice = ordersList.get(0).getRoute().getUnitBuyingPrice();
        }

        boolean hasNullWeight = ordersList.stream()
                .flatMap(order -> order.getWarehouses().stream())
                .anyMatch(warehouse -> warehouse != null && warehouse.getNetWeight() == null);
        if (hasNullWeight) {
            throw new RuntimeException("Một hoặc nhiều đơn hàng chưa được cân, vui lòng kiểm tra lại!");
        }

        BigDecimal totalWeight = ordersList.stream()
                .flatMap(order -> order.getWarehouses().stream())
                .filter(warehouse -> warehouse != null && warehouse.getWeight() != null)
                .map(Warehouse::getNetWeight)
                .map(BigDecimal::valueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAmount = totalWeight.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);

        BigDecimal discount = BigDecimal.ZERO;
        CustomerVoucher customerVoucher = null;
        if (customerVoucherId != null) {
            customerVoucher = customerVoucherRepository.findById(customerVoucherId).orElseThrow(() -> new RuntimeException("Voucher không tồn tại!"));
            Voucher voucher = customerVoucher.getVoucher();
            if (customerVoucher.isUsed()) {
                throw new RuntimeException("Voucher đã sử dụng!");
            }
            if (voucher.getEndDate() != null && LocalDateTime.now().isAfter(voucher.getEndDate())) {
                throw new RuntimeException("Voucher đã hết hạn!");
            }
            if (voucher.getMinOrderValue() != null && totalAmount.compareTo(voucher.getMinOrderValue()) < 0) {
                throw new RuntimeException("Tổng giá trị đơn hàng chưa đạt yêu cầu của voucher!");
            }
            Set<Route> applicableRoutes = voucher.getApplicableRoutes();
            if (!applicableRoutes.isEmpty()) {
                boolean allRoutesMatch = ordersList.stream()
                        .allMatch(order -> applicableRoutes.contains(order.getRoute()));
                if (!allRoutesMatch) {
                    throw new RuntimeException("Voucher không áp dụng cho tuyến của một số đơn hàng!");
                }
            }
            if (voucher.getType() == VoucherType.PHAN_TRAM) {
                discount = totalAmount.multiply(voucher.getValue()).divide(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
            } else if (voucher.getType() == VoucherType.CO_DINH) {
                discount = voucher.getValue();
            }
            totalAmount = totalAmount.subtract(discount);
        }

        BigDecimal totalDebt = ordersList.stream()
                .map(Orders::getLeftoverMoney)
                .filter(leftover -> leftover != null && leftover.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal collect = totalAmount.add(totalDebt).setScale(2, RoundingMode.HALF_UP);

        Payment payment = new Payment();
        payment.setPaymentCode(generateMergedPaymentCode());
        payment.setContent(String.join(", ", orderCodes));
        payment.setPaymentType(PaymentType.MA_QR);
        payment.setAmount(totalAmount);
        payment.setStatus(PaymentStatus.CHO_THANH_TOAN_SHIP);
        payment.setActionAt(LocalDateTime.now());
        payment.setCustomer(commonCustomer);
        payment.setStaff((Staff) accountUtils.getAccountCurrent());
        payment.setIsMergedPayment(true);
        payment.setRelatedOrders(new HashSet<>(ordersList));
        payment.setOrders(null);

        BigDecimal qrAmount = collect;
        BigDecimal balance = (commonCustomer.getBalance() != null) ? commonCustomer.getBalance() : BigDecimal.ZERO;
        BigDecimal usedBalance = BigDecimal.ZERO;
        if (isUseBalance && balance.compareTo(BigDecimal.ZERO) > 0) {
            usedBalance = balance.min(collect);
            commonCustomer.setBalance(balance.subtract(usedBalance));
            qrAmount = collect.subtract(usedBalance).max(BigDecimal.ZERO);
        }

        payment.setCollectedAmount(qrAmount);

        String qrCodeUrl = "https://img.vietqr.io/image/" + bankName + "-" + bankNumber + "-print.png?amount=" + qrAmount + "&addInfo=" + payment.getPaymentCode() + "&accountName=" + bankOwner;
        payment.setQrCode(qrCodeUrl);

        Payment savedPayment = paymentRepository.save(payment);

        for (Orders order : ordersList) {
            order.setLeftoverMoney(BigDecimal.ZERO);
            order.setStatus(OrderStatus.CHO_THANH_TOAN_SHIP);
            ordersService.addProcessLog(order, savedPayment.getPaymentCode(), ProcessLogAction.TAO_THANH_TOAN_HANG);
            ordersRepository.save(order);
        }

        if (isUseBalance && usedBalance.compareTo(BigDecimal.ZERO) > 0) {
            authenticationRepository.save(commonCustomer);
        }

        if (qrAmount.compareTo(BigDecimal.ZERO) == 0) {
            savedPayment.setStatus(PaymentStatus.DA_THANH_TOAN_SHIP);
            savedPayment = paymentRepository.save(savedPayment);
        }
        if (customerVoucher != null) {
            customerVoucher.setUsed(true);
            customerVoucher.setUsedDate(LocalDateTime.now());
            customerVoucherRepository.save(customerVoucher);
        }
        return savedPayment;
    }

 public Payment confirmedPaymentShipment(String paymentCode) {
    System.out.println("=== Start confirmedPaymentShipment ===");
    Optional<Payment> paymentOptional = paymentRepository.findByPaymentCode(paymentCode);

    if (paymentOptional.isEmpty()) {
        System.out.println("[STOP] Không tìm thấy giao dịch!");
        throw new RuntimeException("Không tìm thấy giao dịch này!");
    }

    Payment payment = paymentOptional.get();

    if (!payment.getStatus().equals(PaymentStatus.CHO_THANH_TOAN_SHIP)) {
        System.out.println("[STOP] Trạng thái payment KHÔNG PHẢI CHỜ THANH TOÁN SHIP => DỪNG");
        throw new RuntimeException("Trạng thái đơn hàng không phải chờ thanh toán!");
    }

    payment.setStatus(PaymentStatus.DA_THANH_TOAN_SHIP);
    payment.setCollectedAmount(payment.getAmount());
    payment.setActionAt(LocalDateTime.now());

    if (payment.getIsMergedPayment()) {
        System.out.println("==> Chạy nhánh [MERGED PAYMENT]");
        Set<Orders> orders = payment.getRelatedOrders();
        System.out.println("Số đơn liên quan: " + orders.size());

        for (Orders order : orders) {
            order.setStatus(OrderStatus.CHO_GIAO);
            for (OrderLinks orderLink : order.getOrderLinks()) {
                orderLink.setStatus(OrderLinkStatus.CHO_GIAO);
            }
            ordersRepository.save(order);
            ordersService.addProcessLog(order, payment.getPaymentCode(), ProcessLogAction.DA_THANH_TOAN);
        }
    }

    else {
      
        if (payment.getOrders() != null) {
        
            Orders order = payment.getOrders();
            order.setStatus(OrderStatus.CHO_GIAO);
            for (OrderLinks orderLink : order.getOrderLinks()) {
                orderLink.setStatus(OrderLinkStatus.CHO_GIAO);
            }

            ordersRepository.save(order);
            ordersService.addProcessLog(order, payment.getPaymentCode(), ProcessLogAction.DA_THANH_TOAN);
        } 

        else {
            List<PartialShipment> partialShipments = partialShipmentRepository.findByPayment(payment);

            for (PartialShipment shipment : partialShipments) {
                shipment.setStatus(OrderStatus.CHO_GIAO);
                shipment.setShipmentDate(LocalDateTime.now());

                Set<OrderLinks> readyLinks = shipment.getReadyLinks();

                for (OrderLinks link : readyLinks) {
                    link.setStatus(OrderLinkStatus.CHO_GIAO);
                    link.setPartialShipment(shipment); 
                    orderLinksRepository.save(link);   
                

                partialShipmentRepository.save(shipment);

                Orders order = shipment.getOrders();
                boolean allLinksDone = order.getOrderLinks().stream()
                    .allMatch(l -> l.getStatus() == OrderLinkStatus.CHO_GIAO || l.getStatus() == OrderLinkStatus.DA_HUY || l.getStatus() == OrderLinkStatus.DA_GIAO);

                if (allLinksDone) {
                    order.setStatus(OrderStatus.CHO_GIAO);
                }
            
                ordersRepository.save(order);
                ordersService.addProcessLog(order, payment.getPaymentCode(), ProcessLogAction.DA_THANH_TOAN);
            }
        }
        }
    }

    System.out.println("=== Hoàn tất confirmedPaymentShipment ===");
    return paymentRepository.save(payment);
}
    public Optional<Payment> getPaymentsById(Long paymentId) {
        return paymentRepository.findById(paymentId);
    }

    public List <Payment> getPaymentByStaffandStatus(){
        Staff staff = (Staff) accountUtils.getAccountCurrent();
        return paymentRepository.findAllByStaffAndOrderStatusAndPaymentStatusOrderByActionAtDesc(staff, OrderStatus.CHO_THANH_TOAN_DAU_GIA, PaymentStatus.CHO_THANH_TOAN);
    }

    public Optional<Payment> getPendingPaymentByOrderId(Long orderId) {
        if (!ordersRepository.existsById(orderId)) {
            throw new RuntimeException("Không tìm thấy đơn hàng này!");
        }
        return paymentRepository.findFirstByOrdersOrderIdAndStatus(orderId, PaymentStatus.CHO_THANH_TOAN);
    }

//    public Payment createMergedPayment(Set<String> orderCodes, Integer depositPercent, boolean isUseBalance) {
//        List<Orders> ordersList = ordersRepository.findAllByOrderCodeIn(new ArrayList<>(orderCodes));
//        if (ordersList.size() != orderCodes.size()) {
//            throw new RuntimeException("Một hoặc một số đơn hàng không được tìm thấy!");
//        }
//        if (ordersList.stream().anyMatch(o -> !o.getStatus().equals(OrderStatus.DA_XAC_NHAN))) {
//            throw new RuntimeException("Một hoặc một số đơn hàng chưa đủ điều kiện để thanh toán!");
//        }
//
//        Customer commonCustomer = ordersList.get(0).getCustomer();
//        if (ordersList.stream().anyMatch(o -> !o.getCustomer().equals(commonCustomer))) {
//            throw new RuntimeException("Các đơn hàng phải thuộc cùng một khách hàng để thanh toán gộp!");
//        }
//
//        BigDecimal totalAmount = ordersList.stream()
//                .map(Orders::getFinalPriceOrder)
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        BigDecimal depositRate = BigDecimal.valueOf(depositPercent / 100.00);
//        BigDecimal totalCollect = totalAmount.multiply(depositRate).setScale(2, RoundingMode.HALF_UP);
//
//        Payment payment = new Payment();
//        payment.setPaymentCode(generateMergedPaymentCode());
//        payment.setContent(String.join(" ", orderCodes));
//        payment.setPaymentType(PaymentType.MA_QR);
//        payment.setAmount(totalAmount);
//        payment.setCollectedAmount(totalCollect);
//        payment.setStatus(PaymentStatus.CHO_THANH_TOAN);
//        payment.setActionAt(LocalDateTime.now());
//        payment.setCustomer(commonCustomer);
//        payment.setStaff((Staff) accountUtils.getAccountCurrent());
//        payment.setIsMergedPayment(true);
//        payment.setRelatedOrders(new HashSet<>(ordersList));
//        payment.setOrders(null);
//
//        for (Orders order : ordersList) {
//            BigDecimal orderFinalPrice = order.getFinalPriceOrder();
//            BigDecimal orderCollect = orderFinalPrice.multiply(depositRate).setScale(2, RoundingMode.HALF_UP);
//            BigDecimal orderLeftover = orderFinalPrice.subtract(orderCollect).setScale(2, RoundingMode.HALF_UP);
//            order.setLeftoverMoney(orderLeftover);
//        }
//
//        BigDecimal qrAmount = totalCollect;
//        BigDecimal balance = (commonCustomer.getBalance() != null) ? commonCustomer.getBalance() : BigDecimal.ZERO;
//        if (isUseBalance && balance.compareTo(BigDecimal.ZERO) > 0) {
//            BigDecimal usedBalance = balance.min(totalCollect);
//            commonCustomer.setBalance(balance.subtract(usedBalance));
//            qrAmount = totalCollect.subtract(usedBalance);
//        }
//
//        payment.setCollectedAmount(qrAmount);
//
//        String qrCodeUrl = "https://img.vietqr.io/image/" + bankName + "-" + bankNumber + "-print.png?amount=" + qrAmount + "&addInfo=" + payment.getPaymentCode() + "&accountName=" + bankOwner;
//        payment.setQrCode(qrCodeUrl);
//
//        Payment savedPayment = paymentRepository.save(payment);
//
//        for (Orders order : ordersList) {
//            ordersService.addProcessLog(order, savedPayment.getPaymentCode(), ProcessLogAction.TAO_THANH_TOAN_HANG);
//            order.setStatus(OrderStatus.CHO_THANH_TOAN);
//            ordersRepository.save(order);
//        }
//
//        if (isUseBalance && balance.compareTo(BigDecimal.ZERO) > 0) {
//            authenticationRepository.save(commonCustomer);
//        }
//
//        if (qrAmount.compareTo(BigDecimal.ZERO) == 0 && depositPercent >= 100) {
//            savedPayment.setStatus(PaymentStatus.DA_THANH_TOAN);
//            savedPayment = paymentRepository.save(savedPayment);
//        }
//
//        return savedPayment;
//    }

    public Payment createMergedPayment(Set<String> orderCodes, Integer depositPercent, boolean isUseBalance) {
        List<Orders> ordersList = ordersRepository.findAllByOrderCodeIn(new ArrayList<>(orderCodes));
        if (ordersList.size() != orderCodes.size()) {
            throw new RuntimeException("Một hoặc một số đơn hàng không được tìm thấy!");
        }
        if (ordersList.stream().anyMatch(o -> !o.getStatus().equals(OrderStatus.DA_XAC_NHAN))) {
            throw new RuntimeException("Một hoặc một số đơn hàng chưa đủ điều kiện để thanh toán!");
        }

        Customer commonCustomer = ordersList.get(0).getCustomer();
        if (ordersList.stream().anyMatch(o -> !o.getCustomer().equals(commonCustomer))) {
            throw new RuntimeException("Các đơn hàng phải thuộc cùng một khách hàng để thanh toán gộp!");
        }

        BigDecimal totalAmount = ordersList.stream()
                .map(Orders::getFinalPriceOrder)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal depositRate = BigDecimal.valueOf(depositPercent / 100.00);
        BigDecimal totalCollect = totalAmount.multiply(depositRate).setScale(2, RoundingMode.HALF_UP);

        for (Orders order : ordersList) {
            BigDecimal orderFinalPrice = order.getFinalPriceOrder();
            BigDecimal orderCollect = orderFinalPrice.multiply(depositRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal orderLeftover = orderFinalPrice.subtract(orderCollect).setScale(2, RoundingMode.HALF_UP);
            order.setLeftoverMoney(orderLeftover);
            ordersRepository.save(order);
        }

        Payment payment = new Payment();
        payment.setPaymentCode(generateMergedPaymentCode());
        payment.setContent(String.join(" ", orderCodes));
        payment.setPaymentType(PaymentType.MA_QR);
        payment.setAmount(totalAmount);
        payment.setCollectedAmount(totalCollect);
        payment.setStatus(PaymentStatus.CHO_THANH_TOAN);
        payment.setDepositPercent(depositPercent);
        payment.setActionAt(LocalDateTime.now());
        payment.setCustomer(commonCustomer);
        payment.setStaff((Staff) accountUtils.getAccountCurrent());
        payment.setIsMergedPayment(true);
        payment.setRelatedOrders(new HashSet<>(ordersList));

        BigDecimal qrAmount = totalCollect;
        BigDecimal balance = (commonCustomer.getBalance() != null) ? commonCustomer.getBalance() : BigDecimal.ZERO;
        if (isUseBalance && balance.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal usedBalance = balance.min(totalCollect);
            commonCustomer.setBalance(balance.subtract(usedBalance));
            qrAmount = totalCollect.subtract(usedBalance);
        }

        payment.setCollectedAmount(qrAmount);

        String qrCodeUrl = "https://img.vietqr.io/image/" + bankName + "-" + bankNumber + "-print.png?amount=" + qrAmount + "&addInfo=" + payment.getPaymentCode() + "&accountName=" + bankOwner;
        payment.setQrCode(qrCodeUrl);

        Payment savedPayment = paymentRepository.save(payment);

        for (Orders order : ordersList) {
            ordersService.addProcessLog(order, savedPayment.getPaymentCode(), ProcessLogAction.TAO_THANH_TOAN_HANG);
            order.setStatus(OrderStatus.CHO_THANH_TOAN);
            ordersRepository.save(order);
        }

        if (isUseBalance && balance.compareTo(BigDecimal.ZERO) > 0) {
            authenticationRepository.save(commonCustomer);
        } else if (commonCustomer.getBalance() != null) {
            authenticationRepository.save(commonCustomer);
        }

        if (qrAmount.compareTo(BigDecimal.ZERO) == 0 && depositPercent >= 100) {
            savedPayment.setStatus(PaymentStatus.DA_THANH_TOAN);
            savedPayment = paymentRepository.save(savedPayment);
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
