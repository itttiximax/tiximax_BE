package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.*;
import com.tiximax.txm.Enums.OrderLinkStatus;
import com.tiximax.txm.Enums.OrderStatus;
import com.tiximax.txm.Enums.OrderType;
import com.tiximax.txm.Enums.ProcessLogAction;
import com.tiximax.txm.Model.OrderLinkRequest;
import com.tiximax.txm.Model.OrdersRequest;
import com.tiximax.txm.Repository.*;
import com.tiximax.txm.Utils.AccountUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

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

    public Orders addOrder(String customerCode, Long routeId, OrdersRequest ordersRequest) {
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

        Orders order = new Orders();
        order.setCustomer(customer);
        order.setOrderCode(generateOrderCode(ordersRequest.getOrderType()));
        order.setOrderType(ordersRequest.getOrderType());
        order.setStatus(OrderStatus.DA_XAC_NHAN);
        order.setCreatedAt(LocalDateTime.now());
        order.setExchangeRate(ordersRequest.getExchangeRate());
        order.setDestination(ordersRequest.getDestination());
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
                orderLink.setTotalWeb(linkRequest.getPriceWeb().add(linkRequest.getShipWeb()).multiply(new BigDecimal(linkRequest.getQuantity())).setScale(2, RoundingMode.HALF_UP).add(linkRequest.getPurchaseFee()));
                orderLink.setPurchaseFee(linkRequest.getPurchaseFee());
                orderLink.setProductName(linkRequest.getProductName());
                orderLink.setFinalPriceVnd(orderLink.getTotalWeb().multiply(order.getExchangeRate()));
                orderLink.setPurchaseImage(linkRequest.getPurchaseImage());
                orderLink.setWebsite(linkRequest.getWebsite());
                orderLink.setProductType(linkRequest.getProductType());
                orderLink.setStatus(OrderLinkStatus.HOAT_DONG);
                orderLink.setGroupTag(linkRequest.getGroupTag());
                orderLink.setTrackingCode(generateOrderLinkCode());
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
}
