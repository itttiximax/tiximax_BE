package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.*;
import com.tiximax.txm.Enums.*;
import com.tiximax.txm.Model.*;
import com.tiximax.txm.Repository.*;
import com.tiximax.txm.Utils.AccountUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private SimpMessagingTemplate messagingTemplate;

    public Orders addOrder(String customerCode, Long routeId, OrdersRequest ordersRequest) throws IOException {
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
        Orders order = new Orders();
        order.setCustomer(customer);
        order.setOrderCode(generateOrderCode(ordersRequest.getOrderType()));
        order.setOrderType(ordersRequest.getOrderType());
        order.setStatus(OrderStatus.DA_XAC_NHAN);
        order.setMergedPayment(null);
        order.setCreatedAt(LocalDateTime.now());
        order.setExchangeRate(ordersRequest.getExchangeRate());
        order.setDestination(destination.get());
        order.setCheckRequired(ordersRequest.getCheckRequired());
        order.setRoute(route);
        order.setStaff((Staff) accountUtils.getAccountCurrent());
        BigDecimal totalPriceVnd = BigDecimal.ZERO;

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

                ProductType productType = productTypeRepository.findById(linkRequest.getProductTypeId()).orElseThrow(null);

                orderLink.setFinalPriceVnd(orderLink.getTotalWeb().multiply(order.getExchangeRate()).add(linkRequest.getExtraCharge()).multiply(new BigDecimal(linkRequest.getQuantity())).setScale(2, RoundingMode.HALF_UP));

                orderLink.setWebsite(String.valueOf(linkRequest.getWebsite()));
                orderLink.setProductType(productType);
                orderLink.setStatus(OrderLinkStatus.HOAT_DONG);
                orderLink.setNote(linkRequest.getNote());
                orderLink.setGroupTag(linkRequest.getGroupTag());
                orderLink.setTrackingCode(generateOrderLinkCode());
                orderLink.setPurchaseImage(linkRequest.getPurchaseImage());
                orderLink.setExtraCharge(linkRequest.getExtraCharge());
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
        messagingTemplate.convertAndSend("/topic/orders", order);
        return order;
    }

    public String generateOrderCode(OrderType orderType) {
        String orderCode;
        do {
            if (orderType.equals(OrderType.MUA_HO)){
                orderCode = "MH-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();;
            } else if (orderType.equals(OrderType.KY_GUI)) {
                orderCode = "KG-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();;
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

    public Page<OrderPayment> getOrdersForPayment(Pageable pageable, OrderStatus status) {

        Long staffId = accountUtils.getAccountCurrent().getAccountId();

        List<OrderStatus> validStatuses = Arrays.asList(
                OrderStatus.DA_XAC_NHAN,
                OrderStatus.CHO_THANH_TOAN_SHIP,
                OrderStatus.CHO_THANH_TOAN,
                OrderStatus.CHO_NHAP_KHO_VN);

        if (status == null || !validStatuses.contains(status)) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ!");
        }

        Page<Orders> ordersPage = ordersRepository.findByStaffAccountIdAndStatusForPaymentWithMergedPayment(staffId, status, pageable);

        return ordersPage.map(order -> {
            OrderPayment orderPayment = new OrderPayment(order);
            if (status == OrderStatus.CHO_THANH_TOAN || status == OrderStatus.CHO_THANH_TOAN_SHIP) {
                Optional<Payment> payment = order.getPayments().stream()
                        .filter(p -> p.getStatus() == PaymentStatus.CHO_THANH_TOAN)
                        .findFirst();
                if (payment.isPresent()) {
                    orderPayment.setPaymentCode(payment.get().getPaymentCode());
                } else if (order.getMergedPayment() != null && order.getMergedPayment().getStatus() == PaymentStatus.CHO_THANH_TOAN) {
                    orderPayment.setPaymentCode(order.getMergedPayment().getPaymentCode());
                } else {
                    orderPayment.setPaymentCode(null);
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

    public Page<OrderWithLinks> getOrdersWithLinksForPurchaser(Pageable pageable) {
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

        Page<Orders> ordersPage = ordersRepository.findByRouteRouteIdInAndStatusWithLinks(routeIds, OrderStatus.CHO_MUA, pageable);

        return ordersPage.map(orders -> {
            OrderWithLinks orderWithLinks = new OrderWithLinks(orders);

            List<OrderLinks> sortedLinks = new ArrayList<>(orders.getOrderLinks());
            sortedLinks.sort(Comparator.comparing(
                    (OrderLinks link) -> {
                        if (link.getStatus() == OrderLinkStatus.HOAT_DONG) return 0;
                        if (link.getStatus() == OrderLinkStatus.DA_MUA) return 1;
                        return 2;
                    }
            ).thenComparing(
                    OrderLinks::getGroupTag,
                    Comparator.nullsLast(Comparator.naturalOrder())
            ));

            orderWithLinks.setOrderLinks(sortedLinks);

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
                OrderStatus.CHO_NHAP_KHO_VN,
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

        if (orders.size() < 2){
            throw new IllegalStateException("Khách hàng này không đủ đơn để gộp thanh toán!");
        }

        return orders.stream()
                .map(order -> {
                    OrderPayment orderPayment = new OrderPayment(order);
                    return orderPayment;
                })
                .collect(Collectors.toList());
    }

}
