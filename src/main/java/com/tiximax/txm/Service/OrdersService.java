package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.*;
import com.tiximax.txm.Enums.*;
import com.tiximax.txm.Model.OrderLinkRequest;
import com.tiximax.txm.Model.OrdersRequest;
import com.tiximax.txm.Repository.*;
import com.tiximax.txm.Utils.AccountUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

//    public Orders addOrder(String customerCode, Long routeId, OrdersRequest ordersRequest) {
//        if (customerCode == null){
//            throw new IllegalArgumentException("Bạn phải nhập mã khách hàng để thực hiện hành động này!");
//        }
//        if (routeId == null){
//            throw new IllegalArgumentException("Bạn phải chọn tuyến hàng để tiếp tục!");
//        }
//        Customer customer = authenticationRepository.findByCustomerCode(customerCode);
//        if (customer == null) {
//            throw new IllegalArgumentException("Mã khách hàng không được tìm thấy, vui lòng thử lại!");
//        }
//
//        Route route = routeRepository.findById(routeId).orElseThrow(() -> new RuntimeException("Route not found for ID: " + routeId));
//        Optional<Destination> destination = destinationRepository.findById(ordersRequest.getDestinationId());
//
//        if (destination.isEmpty()) {
//            throw new IllegalArgumentException("Không tìm thấy điểm đến!");
//        }
//        Orders order = new Orders();
//        order.setCustomer(customer);
//        order.setOrderCode(generateOrderCode(ordersRequest.getOrderType()));
//        order.setOrderType(ordersRequest.getOrderType());
//        order.setStatus(OrderStatus.DA_XAC_NHAN);
//        order.setCreatedAt(LocalDateTime.now());
//        order.setExchangeRate(ordersRequest.getExchangeRate());
//        order.setDestination(destination.get());
//        order.setCheckRequired(ordersRequest.getCheckRequired());
//        order.setNote(ordersRequest.getNote());
//        order.setRoute(route);
//        order.setStaff((Staff) accountUtils.getAccountCurrent());
//        order = ordersRepository.save(order);
//
//        BigDecimal totalPriceVnd = BigDecimal.ZERO;
//
//        List<OrderLinks> orderLinksList = new ArrayList<>();
//        if (ordersRequest.getOrderLinkRequests() != null) {
//            for (OrderLinkRequest linkRequest : ordersRequest.getOrderLinkRequests()) {
//                OrderLinks orderLink = new OrderLinks();
//                orderLink.setOrders(order);
//                orderLink.setProductLink(linkRequest.getProductLink());
//                orderLink.setQuantity(linkRequest.getQuantity());
//                orderLink.setPriceWeb(linkRequest.getPriceWeb());
//                orderLink.setShipWeb(linkRequest.getShipWeb());
//                orderLink.setTotalWeb((linkRequest.getPriceWeb().add(linkRequest.getShipWeb())).multiply(new BigDecimal(linkRequest.getQuantity())).setScale(2, RoundingMode.HALF_UP).add(linkRequest.getPurchaseFee()));
//                orderLink.setPurchaseFee(linkRequest.getPurchaseFee());
//                orderLink.setProductName(linkRequest.getProductName());
//
//                ProductType productType = productTypeRepository.findById(linkRequest.getProductTypeId()).orElseThrow(null);
//
//                orderLink.setFinalPriceVnd(orderLink.getTotalWeb().multiply(order.getExchangeRate()).add(linkRequest.getExtraCharge()).multiply(new BigDecimal(linkRequest.getQuantity())).setScale(2, RoundingMode.HALF_UP));
//                orderLink.setPurchaseImage(linkRequest.getPurchaseImage());
//                orderLink.setWebsite(String.valueOf(linkRequest.getWebsite()));
//                orderLink.setProductType(productType);
//                orderLink.setStatus(OrderLinkStatus.HOAT_DONG);
//                orderLink.setGroupTag(linkRequest.getGroupTag());
//                orderLink.setTrackingCode(generateOrderLinkCode());
//                orderLink.setExtraCharge(linkRequest.getExtraCharge());
//                orderLinksList.add(orderLink);
//
//                BigDecimal finalPrice = orderLink.getFinalPriceVnd();
//                if (finalPrice != null) {
//                    totalPriceVnd = totalPriceVnd.add(finalPrice);
//                }
//            }
//            orderLinksRepository.saveAll(orderLinksList);
//        }
//        order.setOrderLinks(new HashSet<>(orderLinksList));
//        order.setFinalPriceOrder(totalPriceVnd);
//        order = ordersRepository.save(order);
//        addProcessLog(order, order.getOrderCode(), ProcessLogAction.XAC_NHAN_DON);
//        return order;
//    }

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
        order.setCreatedAt(LocalDateTime.now());
        order.setExchangeRate(ordersRequest.getExchangeRate());
        order.setDestination(destination.get());
        order.setCheckRequired(ordersRequest.getCheckRequired());
        order.setNote(ordersRequest.getNote());
        order.setRoute(route);
        order.setStaff((Staff) accountUtils.getAccountCurrent());
        order = ordersRepository.save(order);

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
                orderLink.setGroupTag(linkRequest.getGroupTag());
                orderLink.setTrackingCode(generateOrderLinkCode());
//                if (linkRequest.getFile() != null){
//                    orderLink.setPurchaseImage(imageStorageService.uploadImageSupabase(linkRequest.getFile(), orderLink.getTrackingCode()));
//                } else {
//                    orderLink.setPurchaseImage(null);
//                }
                orderLink.setPurchaseImage(linkRequest.getPurchaseImage());
                orderLink.setExtraCharge(linkRequest.getExtraCharge());
                orderLinksList.add(orderLink);

                BigDecimal finalPrice = orderLink.getFinalPriceVnd();
                if (finalPrice != null) {
                    totalPriceVnd = totalPriceVnd.add(finalPrice);
                }
            }
            orderLinksRepository.saveAll(orderLinksList);
        }
        order.setOrderLinks(new HashSet<>(orderLinksList));
        order.setFinalPriceOrder(totalPriceVnd);
        order = ordersRepository.save(order);
        addProcessLog(order, order.getOrderCode(), ProcessLogAction.XAC_NHAN_DON);
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

    public Page<Orders> getOrdersPaging(Pageable pageable) {
        Account currentAccount = accountUtils.getAccountCurrent();
        if (currentAccount.getRole() == AccountRoles.ADMIN || currentAccount.getRole() == AccountRoles.MANAGER) {
            return ordersRepository.findAll(pageable);
        } else if (currentAccount.getRole() == AccountRoles.STAFF_SALE) {
            return ordersRepository.findByStaffAccountId(currentAccount.getAccountId(), pageable);
        } else if (currentAccount.getRole() == AccountRoles.LEAD_SALE) {
////            Staff staff = (Staff) currentAccount;
//            Set<Long> routeIds = staff.getRoutes().stream()
//                    .map(Route::getRouteId)
//                    .collect(Collectors.toSet());
//            if (routeIds.isEmpty()) {
//                return Page.empty(pageable);
//            }
//            return ordersRepository.findByRouteIdIn(routeIds, pageable);
        } else {
            throw new IllegalStateException("Vai trò không hợp lệ!");
        }
        return null;
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
                .filter(order -> order.getStatus() == OrderStatus.CHO_MUA)
                .filter(order -> routeIds.contains(order.getRoute().getRouteId()))
                .collect(Collectors.toList());
    }
}
