package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.*;
import com.tiximax.txm.Enums.*;
import com.tiximax.txm.Model.*;
import com.tiximax.txm.Repository.*;
import com.tiximax.txm.Utils.AccountUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service

public class OrdersService {

    @Autowired
    private AuthenticationRepository authenticationRepository;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private OrderLinksRepository orderLinksRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private AccountUtils accountUtils;
    
    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private ProcessLogRepository processLogRepository;

    @Autowired
    private DestinationRepository destinationRepository;

    @Autowired
    private ProductTypeRepository productTypeRepository;

    @Autowired
    private ImageStorageService imageStorageService;

    @Autowired
    private AccountRouteRepository accountRouteRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private PaymentService paymentService;

    public Orders addOrder(String customerCode, Long routeId, Long addressId, OrdersRequest ordersRequest) throws IOException {
        if (customerCode == null){
            throw new IllegalArgumentException("Bạn phải nhập mã khách hàng để thực hiện hành động này!");
        }
        if (routeId == null){
            throw new IllegalArgumentException("Bạn phải chọn tuyến hàng để tiếp tục!");
        }
        Customer customer = authenticationRepository.findByCustomerCode(customerCode);
        if (customer == null) {
            throw new IllegalArgumentException("Mã khách hàng không được tìm thấy, vui lòng thử lại!");
        }
      
        Route route = routeRepository.findById(routeId).orElseThrow(() -> new RuntimeException("Route not found for ID: " + routeId));
        Optional<Destination> destination = destinationRepository.findById(ordersRequest.getDestinationId());

        if (destination.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy điểm đến!");
        }

        Optional<Address> address = addressRepository.findById(addressId);
        if (address.isEmpty()){
            throw new IllegalArgumentException("Địa chỉ giao hàng cho khách không phù hợp!");
        }

        Orders order = new Orders();
        order.setCustomer(customer);
        order.setAddress(address.get());
        order.setOrderCode(generateOrderCode(ordersRequest.getOrderType()));
        order.setOrderType(ordersRequest.getOrderType());
        order.setStatus(OrderStatus.DA_XAC_NHAN);
        order.setCreatedAt(LocalDateTime.now());
        order.setExchangeRate(ordersRequest.getExchangeRate());
        order.setDestination(destination.get());
        order.setCheckRequired(ordersRequest.getCheckRequired());
        order.setRoute(route);
        order.setStaff((Staff) accountUtils.getAccountCurrent());
        order.setAddress(address.get());
        BigDecimal totalPriceVnd = BigDecimal.ZERO;
        BigDecimal priceBeforeFee = BigDecimal.ZERO;

        List<OrderLinks> orderLinksList = new ArrayList<>();
        if (ordersRequest.getOrderLinkRequests() != null) {
            for (OrderLinkRequest linkRequest : ordersRequest.getOrderLinkRequests()) {
                OrderLinks orderLink = new OrderLinks();
                orderLink.setOrders(order);
                orderLink.setProductLink(linkRequest.getProductLink());
                orderLink.setQuantity(linkRequest.getQuantity());
                orderLink.setPriceWeb(linkRequest.getPriceWeb());
                orderLink.setShipWeb(linkRequest.getShipWeb());
                orderLink.setTotalWeb((linkRequest.getPriceWeb().add(linkRequest.getShipWeb())).multiply(new BigDecimal(linkRequest.getQuantity())).setScale(2, RoundingMode.HALF_UP).add(linkRequest.getPurchaseFee()));
                orderLink.setPurchaseFee(linkRequest.getPurchaseFee());
                orderLink.setProductName(linkRequest.getProductName());
                ProductType productType = productTypeRepository.findById(linkRequest.getProductTypeId())
                        .orElseThrow(() -> new IllegalArgumentException("Kiểu sản phẩm không được tìm thấy"));

                orderLink.setFinalPriceVnd(orderLink.getTotalWeb().multiply(order.getExchangeRate()).add(linkRequest.getExtraCharge()).multiply(new BigDecimal(linkRequest.getQuantity())).setScale(2, RoundingMode.HALF_UP)); 
                orderLink.setWebsite(String.valueOf(linkRequest.getWebsite()));
                orderLink.setProductType(productType);
                orderLink.setClassify(linkRequest.getClassify());
                orderLink.setStatus(OrderLinkStatus.CHO_MUA);
                orderLink.setNote(linkRequest.getNote());
                orderLink.setGroupTag(linkRequest.getGroupTag());
                orderLink.setTrackingCode(generateOrderLinkCode());
                orderLink.setPurchaseImage(linkRequest.getPurchaseImage());
                orderLink.setExtraCharge(linkRequest.getExtraCharge());
                orderLinksList.add(orderLink);
                BigDecimal finalPrice = orderLink.getFinalPriceVnd();
                if (finalPrice != null) {
                    totalPriceVnd = totalPriceVnd.add(finalPrice);
                    priceBeforeFee = priceBeforeFee.add(orderLink.getPriceWeb());
                }
            }
        }
        order.setOrderLinks(new HashSet<>(orderLinksList));
        order.setFinalPriceOrder(totalPriceVnd);
        order.setPriceBeforeFee(priceBeforeFee);
        order = ordersRepository.save(order);
        orderLinksRepository.saveAll(orderLinksList);
        addProcessLog(order, order.getOrderCode(), ProcessLogAction.XAC_NHAN_DON);
        messagingTemplate.convertAndSend(
                "/topic/Tiximax",
                Map.of(
                        "event", "INSERT",
                        "orderCode", order.getOrderCode(),
                        "customerCode", customerCode,
                        "message", "Đơn hàng mới được thêm!"
                )
        );
        return order;
    }

    public Orders addConsignment(String customerCode, Long routeId, Long addressId, ConsignmentRequest consignmentRequest) throws IOException {
        if (customerCode == null){
            throw new IllegalArgumentException("Bạn phải nhập mã khách hàng để thực hiện hành động này!");
        }
        if (routeId == null){
            throw new IllegalArgumentException("Bạn phải chọn tuyến hàng để tiếp tục!");
        }
        Customer customer = authenticationRepository.findByCustomerCode(customerCode);
        if (customer == null) {
            throw new IllegalArgumentException("Mã khách hàng không được tìm thấy, vui lòng thử lại!");
        }

        Route route = routeRepository.findById(routeId).orElseThrow(() -> new RuntimeException("Route not found for ID: " + routeId));
        Optional<Destination> destination = destinationRepository.findById(consignmentRequest.getDestinationId());

        if (destination.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy điểm đến!");
        }

        Optional<Address> address = addressRepository.findById(addressId);
        if (address.isEmpty()){
            throw new IllegalArgumentException("Địa chỉ giao hàng cho khách không phù hợp!");
        }

        Orders order = new Orders();
        order.setCustomer(customer);
        order.setOrderCode(generateOrderCode(consignmentRequest.getOrderType()));
        order.setOrderType(consignmentRequest.getOrderType());
        order.setStatus(OrderStatus.CHO_NHAP_KHO_NN);
        order.setCreatedAt(LocalDateTime.now());
        order.setDestination(destination.get());
        order.setCheckRequired(consignmentRequest.getCheckRequired());
        order.setRoute(route);
        order.setStaff((Staff) accountUtils.getAccountCurrent());
        order.setAddress(address.get());
        BigDecimal totalPriceVnd = BigDecimal.ZERO;

        List<OrderLinks> orderLinksList = new ArrayList<>();
        if (consignmentRequest.getConsignmentLinkRequests() != null) {
            for (ConsignmentLinkRequest linkRequest : consignmentRequest. getConsignmentLinkRequests()) {
                OrderLinks orderLink = new OrderLinks();
                orderLink.setOrders(order);
                orderLink.setQuantity(linkRequest.getQuantity());
                orderLink.setProductName(linkRequest.getProductName());
                ProductType productType = productTypeRepository.findById(linkRequest.getProductTypeId())
                        .orElseThrow(() -> new IllegalArgumentException("Kiểu sản phẩm không được tìm thấy"));
                orderLink.setProductType(productType);
                orderLink.setStatus(OrderLinkStatus.DA_MUA);
                orderLink.setFinalPriceVnd(
                    linkRequest.getExtraCharge()
                        .add(linkRequest.getDifferentFee())
                        .setScale(2, RoundingMode.HALF_UP)
                );     
           orderLink.setNote(linkRequest.getNote());

                String trackingCode = generateOrderLinkCode();
                orderLink.setTrackingCode(trackingCode);
                orderLink.setPurchaseImage(linkRequest.getPurchaseImage());
                orderLink.setShipmentCode(trackingCode);
                orderLinksList.add(orderLink);
                BigDecimal finalPrice = orderLink.getFinalPriceVnd();
                if (finalPrice != null) {
                    totalPriceVnd = totalPriceVnd.add(finalPrice);
                }
            }
        }
        order.setOrderLinks(new HashSet<>(orderLinksList));
        order.setFinalPriceOrder(totalPriceVnd);
        order = ordersRepository.save(order);
        orderLinksRepository.saveAll(orderLinksList);
        addProcessLog(order, order.getOrderCode(), ProcessLogAction.XAC_NHAN_DON);
//        messagingTemplate.convertAndSend("/topic/orders", order);
        return order;
    }

    public Orders updateStatusOrderLink(Long OrderId,Long orderLinkId) {
        Orders order = ordersRepository.findById(OrderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng này!"));
        OrderLinks orderLink = orderLinksRepository.findById(orderLinkId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng link"));
        if (orderLink.getStatus().equals(OrderLinkStatus.DA_MUA)){
            throw new IllegalArgumentException("Sản phẩm đã được mua, không thể hủy được!");
        }

        orderLink.setStatus(OrderLinkStatus.DA_HUY);
        BigDecimal currentLeftover = order.getLeftoverMoney() != null ? order.getLeftoverMoney() : BigDecimal.ZERO;
        order.setLeftoverMoney(currentLeftover.subtract(orderLink.getFinalPriceVnd()));
        orderLinksRepository.save(orderLink);
        ordersRepository.save(order);

        List<OrderLinks> allOrderLinks = orderLinksRepository.findByOrdersOrderId(order.getOrderId());
       
        long countNhapKhoVN = allOrderLinks.stream()
                .filter(link -> link.getStatus() == OrderLinkStatus.DA_NHAP_KHO_VN)
                .count();
        long countDamua = allOrderLinks.stream()
                .filter(link -> link.getStatus() == OrderLinkStatus.DA_MUA)
                .count();
      
        long countCancel = allOrderLinks.stream()
                .filter(link -> link.getStatus() == OrderLinkStatus.DA_HUY)
                .count();
        if (countDamua > 0 && (countDamua + countCancel == allOrderLinks.size())) {
            order.setStatus(OrderStatus.CHO_NHAP_KHO_NN);
            ordersRepository.save(order);
        }
        if (countNhapKhoVN > 0 && (countNhapKhoVN + countCancel == allOrderLinks.size())) {
            
            allOrderLinks.stream()
                    .filter(link -> link.getStatus() == OrderLinkStatus.DA_NHAP_KHO_VN)
                    .forEach(link -> {
                        link.setStatus(OrderLinkStatus.CHO_GIAO);
                        orderLinksRepository.save(link);
                    });
            order.setStatus(OrderStatus.DA_DU_HANG);
            ordersRepository.save(order);
        } else if (countCancel == allOrderLinks.size()) {
            order.setStatus(OrderStatus.DA_HUY);
            ordersRepository.save(order);
        }
//        addProcessLog(order, order.getOrderCode(), ProcessLogAction.DA_HUY_LINK);
        return order; 
    }
  
    public String generateOrderCode(OrderType orderType) {
        String orderCode;
        do {
            if (orderType.equals(OrderType.MUA_HO)){
                orderCode = "MH-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();;
            } else if (orderType.equals(OrderType.KY_GUI)) {
                orderCode = "KG-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();;
            } else if (orderType.equals(OrderType.DAU_GIA)) {
                orderCode = "DG-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();;
            } else {
                throw new IllegalStateException("Không có kiểu đơn hàng " + orderType);
            }
        } while (ordersRepository.existsByOrderCode(orderCode));
        return orderCode;
    }

    public String generateOrderLinkCode() {
        String orderLinkCode;
        do {
            orderLinkCode = "DH-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();;
        } while (orderLinksRepository.existsByTrackingCode(orderLinkCode));
        return orderLinkCode;
    }

    public void addProcessLog(Orders orders, String actionCode, ProcessLogAction processLogAction){
        OrderProcessLog orderProcessLog = new OrderProcessLog();
        orderProcessLog.setOrders(orders);
        orderProcessLog.setStaff((Staff) accountUtils.getAccountCurrent());
        orderProcessLog.setAction(processLogAction);
        orderProcessLog.setActionCode(actionCode);
        orderProcessLog.setTimestamp(LocalDateTime.now());
        orderProcessLog.setRoleAtTime(((Staff) accountUtils.getAccountCurrent()).getRole());
        processLogRepository.save(orderProcessLog);
    }

    public List<Orders> getAllOrders() {
        return ordersRepository.findAll();
    }

    public Page<Orders> getOrdersPaging(Pageable pageable, OrderStatus status) {
        Account currentAccount = accountUtils.getAccountCurrent();
        if (currentAccount.getRole().equals(AccountRoles.ADMIN) || currentAccount.getRole().equals(AccountRoles.MANAGER)) {
            return ordersRepository.findByStatus(status, pageable);
        } else if (currentAccount.getRole().equals(AccountRoles.STAFF_SALE)) {
            return ordersRepository.findByStaffAccountIdAndStatus(currentAccount.getAccountId(), status, pageable);
        } else if (currentAccount.getRole().equals(AccountRoles.LEAD_SALE)) {
            List<AccountRoute> accountRoutes = accountRouteRepository.findByAccountAccountId(currentAccount.getAccountId());
            Set<Long> routeIds = accountRoutes.stream()
                    .map(AccountRoute::getRoute)
                    .map(Route::getRouteId)
                    .collect(Collectors.toSet());
            if (routeIds.isEmpty()) {
                return Page.empty(pageable);
            }
            return ordersRepository.findByRouteRouteIdInAndStatus(routeIds, status, pageable);
        } else {
            throw new IllegalStateException("Vai trò không hợp lệ!");
        }
    }

    public List<Orders> getOrdersForCurrentStaff() {

        Account currentAccount = accountUtils.getAccountCurrent();
        if (!(currentAccount instanceof Staff)) {
            throw new IllegalStateException("Tài khoản hiện tại không phải là nhân viên!");
        }

        List<AccountRoute> accountRoutes = accountRouteRepository.findByAccountAccountId(currentAccount.getAccountId());
        if (accountRoutes.isEmpty()) {
            return List.of();
        }

        List<Long> routeIds = accountRoutes.stream()
                .map(AccountRoute::getRoute)
                .map(Route::getRouteId)
                .collect(Collectors.toList());

        return ordersRepository.findAll().stream()
                .filter(order -> order.getStatus().equals(OrderStatus.CHO_MUA))
                .filter(order -> routeIds.contains(order.getRoute().getRouteId()))
                .collect(Collectors.toList());
    }

    public Page<OrderPayment> getOrdersForPayment(Pageable pageable, OrderStatus status ) {
        Long staffId = accountUtils.getAccountCurrent().getAccountId();

        List<OrderStatus> validStatuses = Arrays.asList(
                OrderStatus.DA_XAC_NHAN,
                OrderStatus.CHO_THANH_TOAN,
                OrderStatus.DA_DU_HANG,
                OrderStatus.CHO_THANH_TOAN_DAU_GIA,
                OrderStatus.CHO_THANH_TOAN_SHIP);

        if (status == null || !validStatuses.contains(status)) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ!");
        }

        Page<Orders> ordersPage = ordersRepository.findByStaffAccountIdAndStatusForPayment(staffId, status, pageable);
        
        return ordersPage.map(order -> {
            OrderPayment orderPayment = new OrderPayment(order);
             if (status == OrderStatus.CHO_THANH_TOAN_DAU_GIA) {
            Optional<Payment> payment = order.getPayments().stream()
                    .filter(p -> p.getStatus() == PaymentStatus.CHO_THANH_TOAN)
                    .findFirst();
            orderPayment.setPaymentCode(payment.map(Payment::getPaymentCode).orElse(null));
            return orderPayment;
        }

            if (status == OrderStatus.DA_DU_HANG || status == OrderStatus.CHO_THANH_TOAN_SHIP) {
                BigDecimal totalNetWeight = order.getWarehouses().stream()
                        .map(warehouse -> BigDecimal.valueOf(warehouse.getNetWeight()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .setScale(2, RoundingMode.HALF_UP);

                if (totalNetWeight.compareTo(BigDecimal.valueOf(0.5)) < 0) {
                    totalNetWeight = BigDecimal.valueOf(0.5);
                } else if (totalNetWeight.compareTo(BigDecimal.valueOf(0.5)) >= 0 && totalNetWeight.compareTo(BigDecimal.ONE) < 0) {
                    totalNetWeight = BigDecimal.ONE;
                }

                orderPayment.setTotalNetWeight(totalNetWeight);
                if (order.getExchangeRate() != null) {
                    BigDecimal calculatedPrice = totalNetWeight.multiply(order.getRoute().getUnitBuyingPrice()).setScale(2, RoundingMode.HALF_UP);
                    orderPayment.setFinalPriceOrder(calculatedPrice);
                } else {
                    orderPayment.setFinalPriceOrder(null);
                }
            }
            if (status == OrderStatus.CHO_THANH_TOAN || status == OrderStatus.CHO_THANH_TOAN_SHIP) {
                Optional<Payment> payment = order.getPayments().stream()
                        .filter(p -> p.getStatus() == PaymentStatus.CHO_THANH_TOAN || p.getStatus() == PaymentStatus.CHO_THANH_TOAN_SHIP)
                        .findFirst();

                if (payment.isPresent()) {
                    orderPayment.setPaymentCode(payment.get().getPaymentCode());
                } else {
                    Optional<Payment> mergedPayment = paymentRepository.findMergedPaymentByOrderIdAndStatus(order.getOrderId(), PaymentStatus.CHO_THANH_TOAN);
                    if (!mergedPayment.isPresent()) {
                        mergedPayment = paymentRepository.findMergedPaymentByOrderIdAndStatus(order.getOrderId(), PaymentStatus.CHO_THANH_TOAN_SHIP);
                    }
                    orderPayment.setPaymentCode(mergedPayment.map(Payment::getPaymentCode).orElse(null));
                }
            } else {
                orderPayment.setPaymentCode(null);
            }
            return orderPayment;
        });
    }

    public OrderDetail getOrderDetail(Long orderId) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng này!"));
        return new OrderDetail(order);
    }

    public Page<OrderWithLinks> getOrdersWithLinksForPurchaser(Pageable pageable, OrderType orderType) {
        Account currentAccount = accountUtils.getAccountCurrent();

        if (!currentAccount.getRole().equals(AccountRoles.STAFF_PURCHASER)) {
            throw new IllegalStateException("Chỉ nhân viên mua hàng mới có quyền truy cập!");
        }

        List<AccountRoute> accountRoutes = accountRouteRepository.findByAccountAccountId(currentAccount.getAccountId());
        Set<Long> routeIds = accountRoutes.stream()
                .map(AccountRoute::getRoute)
                .map(Route::getRouteId)
                .collect(Collectors.toSet());

        if (routeIds.isEmpty()) {
            return Page.empty(pageable);
        }

        Sort sort = Sort.by(Sort.Order.desc("pinnedAt").nullsLast())
                .and(Sort.by(Sort.Order.desc("createdAt")));
        Pageable customPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        Page<Orders> ordersPage = ordersRepository.findByRouteRouteIdInAndStatusAndOrderTypeWithLinks(routeIds, OrderStatus.CHO_MUA, orderType, customPageable);

        return ordersPage.map(orders -> {
            OrderWithLinks orderWithLinks = new OrderWithLinks(orders);

            List<OrderLinks> sortedLinks = new ArrayList<>(orders.getOrderLinks());
            sortedLinks.sort(Comparator.comparing(
                    (OrderLinks link) -> {
                        if (link.getStatus() == OrderLinkStatus.CHO_MUA) return 0;
                        if (link.getStatus() == OrderLinkStatus.DA_MUA) return 1;
                        return 2;
                    }
            ).thenComparing(
                    OrderLinks::getGroupTag,
                    Comparator.nullsLast(Comparator.naturalOrder())
            ));

            orderWithLinks.setOrderLinks(sortedLinks);
            orderWithLinks.setPinnedAt(orders.getPinnedAt());
            return orderWithLinks;
        });
    }

    public OrderLinks getOrderLinkById(Long orderLinkId) {
        OrderLinks orderLink = orderLinksRepository.findById(orderLinkId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm này!"));
        return orderLink;
    }
    
    public Map<String, Long> getOrderStatusStatistics() {
        Account currentAccount = accountUtils.getAccountCurrent();
        if (!(currentAccount instanceof Staff)) {
            throw new IllegalStateException("Chỉ nhân viên mới có quyền truy cập thống kê này!");
        }
        Long staffId = currentAccount.getAccountId();

        List<OrderStatus> statusesToCount = Arrays.asList(
                OrderStatus.DA_XAC_NHAN,
                OrderStatus.CHO_THANH_TOAN,
                OrderStatus.CHO_THANH_TOAN_DAU_GIA,
                OrderStatus.DA_DU_HANG,
                OrderStatus.CHO_THANH_TOAN_SHIP
        );

        Map<String, Long> statistics = new HashMap<>();
        for (OrderStatus status : statusesToCount) {
            long count = ordersRepository.countByStaffAccountIdAndStatus(staffId, status);
            statistics.put(status.name(), count);
        }

        return statistics;
    }

    public List<OrderPayment> getOrdersByCustomerCode(String customerCode) {
        Customer customer = authenticationRepository.findByCustomerCode(customerCode);
        if (customer == null) {
            throw new IllegalArgumentException("Mã khách hàng không được tìm thấy, vui lòng thử lại!");
        }

        if (!customer.getStaffId().equals(accountUtils.getAccountCurrent().getAccountId())) {
            throw new IllegalStateException("Bạn không có quyền truy cập đơn hàng của khách hàng này!");
        }

        List<Orders> orders = ordersRepository.findByCustomerCodeAndStatus(customerCode, OrderStatus.DA_XAC_NHAN);

//        if (orders.size() < 2){
//            throw new IllegalStateException("Khách hàng này không đủ đơn để gộp thanh toán!");
//        }

        return orders.stream()
                .map(order -> {
                    OrderPayment orderPayment = new OrderPayment(order);
                    return orderPayment;
                })
                .collect(Collectors.toList());
    }

      public List<OrderLinks> getLinksInWarehouseByCustomer(String customerCode) {

         Customer customer = authenticationRepository.findByCustomerCode(customerCode);
        if (customer == null) {
            throw new IllegalArgumentException("Mã khách hàng không được tìm thấy, vui lòng thử lại!");
        }

        if (!customer.getStaffId().equals(accountUtils.getAccountCurrent().getAccountId())) {
            throw new IllegalStateException("Bạn không có quyền truy cập đơn hàng của khách hàng này!");
        }
        
        return orderLinksRepository.findByCustomerCodeAndShipmentCodeNotNullAndStatus(
                customerCode,
                OrderLinkStatus.DA_NHAP_KHO_VN
        );
    }

    public List<OrderPayment> getOrdersShippingByCustomerCode(String customerCode) {
        Customer customer = authenticationRepository.findByCustomerCode(customerCode);
        if (customer == null) {
            throw new IllegalArgumentException("Mã khách hàng không được tìm thấy, vui lòng thử lại!");
        }

        if (!customer.getStaffId().equals(accountUtils.getAccountCurrent().getAccountId())) {
            throw new IllegalStateException("Bạn không có quyền truy cập đơn hàng của khách hàng này!");
        }

        List<Orders> orders = ordersRepository.findByCustomerCodeAndStatus(customerCode, OrderStatus.DA_DU_HANG);

        return orders.stream()
                .map(order -> {
                    OrderPayment orderPayment = new OrderPayment(order);

                    BigDecimal totalNetWeight = order.getWarehouses() != null
                            ? order.getWarehouses().stream()
                            .map(Warehouse::getNetWeight)
                            .filter(netWeight -> netWeight != null)
                            .map(BigDecimal::valueOf)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            : BigDecimal.ZERO;
                    orderPayment.setTotalNetWeight(totalNetWeight);

                    Route route = order.getRoute();
                    BigDecimal unitPrice = (order.getOrderType() == OrderType.KY_GUI && route.getUnitDepositPrice() != null)
                            ? route.getUnitDepositPrice()
                            : route.getUnitBuyingPrice() != null ? route.getUnitBuyingPrice() : BigDecimal.ZERO;
                    BigDecimal finalPriceOrder = totalNetWeight.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
                    orderPayment.setFinalPriceOrder(finalPriceOrder);
                    orderPayment.setLeftoverMoney(order.getLeftoverMoney());
                    return orderPayment;
                })
                .collect(Collectors.toList());
    }

    public Orders updateOrderLinkToBuyLater(Long orderId, Long orderLinkId) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng này!"));

        OrderLinks orderLink = orderLinksRepository.findById(orderLinkId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy link sản phẩm!"));

        if (!orderLink.getStatus().equals(OrderLinkStatus.CHO_MUA)) {
            throw new IllegalArgumentException("Chỉ có thể chuyển sang MUA SAU nếu trạng thái hiện tại là CHỜ MUA!");
        }

        orderLink.setStatus(OrderLinkStatus.MUA_SAU);
        orderLinksRepository.save(orderLink);

        List<OrderLinks> allOrderLinks = orderLinksRepository.findByOrdersOrderId(order.getOrderId());

        long countMuaSau = allOrderLinks.stream()
                .filter(link -> link.getStatus() == OrderLinkStatus.MUA_SAU)
                .count();

        long countDaHuy = allOrderLinks.stream()
                .filter(link -> link.getStatus() == OrderLinkStatus.DA_HUY)
                .count();

        long countDaMua = allOrderLinks.stream()
                .filter(link -> link.getStatus() == OrderLinkStatus.DA_MUA)
                .count();

        if (countMuaSau + countDaHuy == allOrderLinks.size()) {
            order.setStatus(OrderStatus.CHO_MUA);
            ordersRepository.save(order);
        } else if (countDaMua > 0 && (countDaMua + countDaHuy + countMuaSau == allOrderLinks.size())) {
            order.setStatus(OrderStatus.CHO_NHAP_KHO_NN);
            ordersRepository.save(order);
        }

//        addProcessLog(order, order.getOrderCode(), ProcessLogAction.CAP_NHAT_TRANG_THAI_LINK);
        return order;
    }

    public void pinOrder(Long orderId, boolean pin) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng!"));
        order.setPinnedAt(pin ? LocalDateTime.now() : null);
        messagingTemplate.convertAndSend(
                "/topic/Tiximax",
                Map.of(
                        "event", pin ? "PIN" : "UNPIN",
                        "orderCode", order.getOrderCode(),
                        "customerCode", order.getCustomer().getCustomerCode(),
                        "message", pin ? "Đơn hàng đã được ghim!" : "Đơn hàng đã được bỏ ghim!"
                )
        );
        ordersRepository.save(order);
    }

//    public List<Orders> getReadyOrdersForPartial(Pageable pageable) {
//        Page<Orders> ordersPage = ordersRepository.findByStatus(OrderStatus.DA_DU_HANG, pageable);
//        return ordersPage.getContent().stream()
//                .filter(order -> order.getOrderLinks().stream().anyMatch(link -> link.getStatus() == OrderLinkStatus.DA_NHAP_KHO_VN))
//                .collect(Collectors.toList());
//    }

//    public List<Orders> getReadyOrdersForPartial(Pageable pageable) {
//        List<OrderStatus> statuses = Arrays.asList(OrderStatus.DA_DU_HANG, OrderStatus.DANG_XU_LY);
//        Page<Orders> ordersPage = ordersRepository.findByStatusIn(statuses, pageable);
//        return ordersPage.getContent().stream()
//                .filter(order -> order.getOrderLinks().stream().anyMatch(link -> link.getStatus() == OrderLinkStatus.DA_NHAP_KHO_VN))
//                .collect(Collectors.toList());
//    }

    public List<OrderPayment> getReadyOrdersForPartial(Pageable pageable) {
        List<OrderStatus> statuses = Arrays.asList(OrderStatus.DA_DU_HANG, OrderStatus.DANG_XU_LY);
        Page<Orders> ordersPage = ordersRepository.findByStatusIn(statuses, pageable);

        return ordersPage.getContent().stream()
                .filter(order -> order.getOrderLinks().stream()
                        .anyMatch(link -> link.getStatus() == OrderLinkStatus.DA_NHAP_KHO_VN))
                .map(order -> {
                    OrderPayment orderPayment = new OrderPayment(order);

                    BigDecimal totalNetWeight = order.getOrderLinks().stream()
                            .filter(link -> link.getStatus() == OrderLinkStatus.DA_NHAP_KHO_VN)
                            .map(OrderLinks::getWarehouse)
                            .filter(Objects::nonNull)
                            .map(Warehouse::getNetWeight)
                            .filter(Objects::nonNull)
                            .map(BigDecimal::valueOf)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    orderPayment.setTotalNetWeight(totalNetWeight);

                    Route route = order.getRoute();
                    BigDecimal unitPrice = (order.getOrderType() == OrderType.KY_GUI && route.getUnitDepositPrice() != null)
                            ? route.getUnitDepositPrice()
                            : route.getUnitBuyingPrice() != null ? route.getUnitBuyingPrice() : BigDecimal.ZERO;

                    BigDecimal finalPriceOrder = totalNetWeight.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
                    orderPayment.setFinalPriceOrder(finalPriceOrder);
                    orderPayment.setLeftoverMoney(order.getLeftoverMoney());

                    return orderPayment;
                })
                .collect(Collectors.toList());
    }

    public Page<Orders> getOrdersWithNegativeLeftoverMoney(Pageable pageable) {
        Account currentAccount = accountUtils.getAccountCurrent();
        if (!(currentAccount instanceof Staff)) {
            throw new IllegalStateException("Chỉ nhân viên mới có quyền truy cập danh sách đơn hàng này!");
        }
        Staff staff = (Staff) currentAccount;
        Long staffId = staff.getAccountId();

        List<OrderStatus> statuses = Arrays.asList(OrderStatus.DA_HUY, OrderStatus.DA_GIAO);

        AccountRoles role = staff.getRole();

        if (AccountRoles.MANAGER.equals(role)) {
            return ordersRepository.findByStatusInAndLeftoverMoneyLessThan(statuses, BigDecimal.ZERO, pageable);
        } else if (AccountRoles.STAFF_SALE.equals(role) || AccountRoles.LEAD_SALE.equals(role)) {
            return ordersRepository.findByStaffAccountIdAndStatusInAndLeftoverMoneyLessThan(staffId, statuses, BigDecimal.ZERO, pageable);
        } else {
            throw new IllegalStateException("Vai trò không hợp lệ!");
        }
    }

    public Orders processNegativeLeftoverMoney(Long orderId, boolean refundToCustomer) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng này!"));

        if (order.getLeftoverMoney() == null || order.getLeftoverMoney().compareTo(BigDecimal.ZERO) >= 0) {
            throw new IllegalArgumentException("Đơn hàng này không có tiền hoàn trả!");
        }

        List<OrderStatus> validStatuses = Arrays.asList(OrderStatus.DA_HUY, OrderStatus.DA_GIAO);
        if (!validStatuses.contains(order.getStatus())) {
            throw new IllegalArgumentException("Chỉ xử lý được đơn hàng trạng thái DA_HUY hoặc DA_GIAO!");
        }

        BigDecimal amountToProcess = order.getLeftoverMoney().abs();
        Customer customer = order.getCustomer();

        Payment refundPayment = new Payment();
        refundPayment.setPaymentCode(paymentService.generatePaymentCode());
        refundPayment.setPaymentType(PaymentType.MA_QR);
        refundPayment.setAmount(amountToProcess.negate());
        refundPayment.setCollectedAmount(BigDecimal.ZERO);
        refundPayment.setStatus(PaymentStatus.DA_HOAN_TIEN);
        refundPayment.setActionAt(LocalDateTime.now());
        refundPayment.setCustomer(customer);
        refundPayment.setStaff((Staff) accountUtils.getAccountCurrent());
        refundPayment.setOrders(order);
        refundPayment.setIsMergedPayment(false);

        if (refundToCustomer) {
            refundPayment.setContent("Hoàn tiền cho đơn " + order.getOrderCode());
            paymentRepository.save(refundPayment);
        } else {
            customer.setBalance(customer.getBalance().add(amountToProcess));
            refundPayment.setContent("Chuyển vào số dư cho đơn " + order.getOrderCode());
            paymentRepository.save(refundPayment);
        }

        order.setLeftoverMoney(BigDecimal.ZERO);

        authenticationRepository.save(customer);
        ordersRepository.save(order);

        addProcessLog(order, order.getOrderCode(), ProcessLogAction.HOAN_TIEN);
        return order;
    }
}