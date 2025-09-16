package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.*;
import com.tiximax.txm.Enums.OrderStatus;
import com.tiximax.txm.Enums.ProcessLogAction;
import com.tiximax.txm.Enums.WarehouseStatus;
import com.tiximax.txm.Model.WarehouseRequest;
import com.tiximax.txm.Repository.OrderLinksRepository;
import com.tiximax.txm.Repository.OrdersRepository;
import com.tiximax.txm.Repository.PurchasesRepository;
import com.tiximax.txm.Repository.WarehouseRepository;
import com.tiximax.txm.Utils.AccountUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class WarehouseService {

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private PurchasesRepository purchasesRepository;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private OrderLinksRepository orderLinksRepository;

    @Autowired
    private OrdersService ordersService;

    @Autowired
    private AccountUtils accountUtils;

//    public List<Warehouse> createWarehouseEntry(Long purchaseId, Long locationId, WarehouseRequest warehouseRequest) {
//        Purchases purchase = purchasesRepository.findById(purchaseId)
//                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn mua hàng này!"));
//
//        Orders order = purchase.getOrders();
//        if (order == null) {
//            throw new IllegalArgumentException("Không tìm thấy đơn hàng liên quan đến đơn mua này!");
//        }
//
//        if (!order.getStatus().equals(OrderStatus.CHO_NHAP_KHO_NN)){
//            throw new RuntimeException("Đơn hàng chưa đủ điều kiện để nhập kho!");
//        }
//
//        List<OrderLinks> orderLinks = orderLinksRepository.findByPurchasePurchaseId(purchaseId);
//        if (orderLinks.isEmpty()) {
//            throw new IllegalArgumentException("Không tìm thấy mã sản phẩm cho đơn mua hàng này!");
//        }
//
//        List<WarehouseRequest.ProductDetail> productDetails = warehouseRequest.getProducts();
//        if (productDetails.size() != orderLinks.size()) {
//            throw new IllegalArgumentException("Số lượng sản phẩm trong yêu cầu kho (" + productDetails.size() +
//                    ") không khớp với số lượng sản phẩm thực (" + orderLinks.size() + ")!");
//        }
//
//        Object currentAccount = accountUtils.getAccountCurrent();
//        if (!(currentAccount instanceof Staff)) {
//            throw new IllegalStateException("Chỉ nhân viên được phép tạo mục kho.");
//        }
//        Staff staff = (Staff) currentAccount;
//
//        List<Warehouse> warehouses = new ArrayList<>();
//        for (int i = 0; i < orderLinks.size(); i++) {
//            OrderLinks orderLink = orderLinks.get(i);
//            WarehouseRequest.ProductDetail productDetail = productDetails.get(i);
//
//            if (!orderLink.getLinkId().equals(productDetail.getOrderLinkId())) {
//                throw new IllegalArgumentException("Mã sản phẩm " + productDetail.getOrderLinkId() +
//                        " không khớp với sản phẩm tại vị trí " + i);
//            }
//
//            if (warehouseRepository.existsByPurchasePurchaseIdAndOrderLinkLinkId(purchaseId, orderLink.getLinkId())) {
//                throw new IllegalArgumentException("Mục kho đã tồn tại cho sản phẩm với mã: " + orderLink.getTrackingCode());
//            }
//
//            Double dim = (productDetail.getLength() * productDetail.getWidth() * productDetail.getHeight()) / 5000;
//
//            Warehouse warehouse = new Warehouse();
//            warehouse.setTrackingCode(orderLink.getTrackingCode());
//            warehouse.setLength(productDetail.getLength());
//            warehouse.setWidth(productDetail.getWidth());
//            warehouse.setHeight(productDetail.getHeight());
//            warehouse.setDim(dim);
//            warehouse.setWeight(productDetail.getWeight());
//            warehouse.setNetWeight(productDetail.getNetWeight());
//            warehouse.setStatus(WarehouseStatus.DA_NHAP_KHO);
//            warehouse.setCreatedAt(LocalDateTime.now());
//            warehouse.setStaff(staff);
//            warehouse.setOrders(order);
//            warehouse.setPurchase(purchase);
//            warehouse.setOrderLink(orderLink);
//            warehouse.setPacking(null);
//
//            WarehouseLocation location = new WarehouseLocation();
//            location.setLocationId(locationId);
//            warehouse.setLocation(location);
//
//            warehouses.add(warehouse);
//            ordersService.addProcessLog(order, purchase.getPurchaseCode(), ProcessLogAction.DA_NHAP_KHO_NN);
//        }
//
//        warehouses = warehouseRepository.saveAll(warehouses);
//
//        List<OrderLinks> allOrderLinks = orderLinksRepository.findByOrdersOrderId(order.getOrderId());
//        boolean allItemsReceived = allOrderLinks.stream()
//                .allMatch(orderLink -> warehouseRepository.existsByOrderLinkLinkId(orderLink.getLinkId()));
//
//        if (allItemsReceived) {
//            order.setStatus(OrderStatus.CHO_THANH_TOAN_SHIP);
//            ordersRepository.save(order);
//        }
//
//        return warehouses;
//    }


//    public List<Warehouse> createWarehouseEntry(String shipmentCode, WarehouseRequest warehouseRequest) {
//        Optional<OrderLinks> orderLinkOptional = orderLinksRepository.findByShipmentCode(shipmentCode);
//        if (orderLinkOptional.isEmpty()) {
//            throw new IllegalArgumentException("Không tìm thấy mã sản phẩm với shipmentCode: " + shipmentCode + "!");
//        }
//
//        OrderLinks orderLink = orderLinkOptional.get();
//        Orders order = orderLink.getOrders();
//        if (order == null) {
//            throw new IllegalArgumentException("Không tìm thấy đơn hàng liên quan đến mã sản phẩm này!");
//        }
//
//        if (!order.getStatus().equals(OrderStatus.CHO_NHAP_KHO_NN)) {
//            throw new RuntimeException("Đơn hàng chưa đủ điều kiện để nhập kho!");
//        }
//
//        // Giả định WarehouseRequest cho một orderLink, nên productDetails.size() == 1
//        List<WarehouseRequest.ProductDetail> productDetails = warehouseRequest.getProducts();
//        if (productDetails.size() != 1) {
//            throw new IllegalArgumentException("Yêu cầu nhập kho cho shipmentCode chỉ hỗ trợ một sản phẩm!");
//        }
//
//        List<Warehouse> warehouses = new ArrayList<>();
//        WarehouseRequest.ProductDetail productDetail = productDetails.get(0);
//
//        Warehouse warehouse = new Warehouse();
//        warehouse.setTrackingCode(orderLink.getShipmentCode());
//        warehouse.setWeight(productDetail.getWeight());
//        warehouse.setNetWeight(productDetail.getNetWeight());
//        warehouse.setStatus(WarehouseStatus.DA_NHAP_KHO);
//        warehouse.setCreatedAt(LocalDateTime.now());
//
//        Staff staff = (Staff) accountUtils.getAccountCurrent();
//        warehouse.setStaff(staff);
//
//        WarehouseLocation location = new WarehouseLocation();
//        location.setLocationId(staff.getWarehouseLocation().getLocationId());
//        warehouse.setLocation(location);
//
//        warehouse.setOrders(order);
//        warehouse.setPurchase(orderLink.getPurchase());
//        warehouse.setOrderLink(orderLink);
//
//        warehouses.add(warehouse);
//        ordersService.addProcessLog(order, orderLink.getTrackingCode(), ProcessLogAction.DA_NHAP_KHO_NN);
//
//        warehouses = warehouseRepository.saveAll(warehouses);
//
//        List<OrderLinks> allOrderLinks = orderLinksRepository.findByOrdersOrderId(order.getOrderId());
//        boolean allItemsReceived = allOrderLinks.stream()
//                .allMatch(ol -> warehouseRepository.existsByOrderLinkLinkId(ol.getLinkId()));
//
//        if (allItemsReceived) {
//            order.setStatus(OrderStatus.CHO_THANH_TOAN_SHIP);
//            ordersRepository.save(order);
//        }
//
//        return warehouses;
//    }

    public List<Warehouse> createWarehouseEntry(String shipmentCode, WarehouseRequest warehouseRequest) {

        List<OrderLinks> orderLinks = orderLinksRepository.findByShipmentCode(shipmentCode);
        if (orderLinks.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy mã sản phẩm với mã vận đơn này!");
        }

        Orders order = orderLinks.get(0).getOrders();
        if (order == null) {
            throw new IllegalArgumentException("Không tìm thấy đơn hàng liên quan đến mã sản phẩm này!");
        }

        if (!order.getStatus().equals(OrderStatus.CHO_NHAP_KHO_NN)) {
            throw new RuntimeException("Đơn hàng chưa đủ điều kiện để nhập kho!");
        }

        List<WarehouseRequest.ProductDetail> productDetails = warehouseRequest.getProducts();
        if (productDetails.isEmpty()) {
            throw new IllegalArgumentException("Yêu cầu nhập kho không chứa thông tin sản phẩm!");
        }

        if (productDetails.size() != orderLinks.size()) {
            throw new IllegalArgumentException("Số lượng sản phẩm trong yêu cầu (" + productDetails.size() +
                    ") không khớp với số lượng sản phẩm thực (" + orderLinks.size() + ") cho shipmentCode: " + shipmentCode + "!");
        }

        List<Warehouse> warehouses = new ArrayList<>();
        for (int i = 0; i < orderLinks.size(); i++) {
            OrderLinks orderLink = orderLinks.get(i);
            WarehouseRequest.ProductDetail productDetail = productDetails.get(i);

            Warehouse warehouse = new Warehouse();
            warehouse.setTrackingCode(orderLink.getShipmentCode()); // Sử dụng shipmentCode từ OrderLink
            warehouse.setWeight(productDetail.getWeight());
            warehouse.setNetWeight(productDetail.getNetWeight());
            warehouse.setStatus(WarehouseStatus.DA_NHAP_KHO);
            warehouse.setCreatedAt(LocalDateTime.now());

            Staff staff = (Staff) accountUtils.getAccountCurrent();
            warehouse.setStaff(staff);

            // Lấy locationId từ warehouseLocation của staff hiện tại
            if (staff.getWarehouseLocation() == null) {
                throw new IllegalArgumentException("Nhân viên hiện tại chưa được gán WarehouseLocation!");
            }
            WarehouseLocation location = new WarehouseLocation();
            location.setLocationId(staff.getWarehouseLocation().getLocationId());
            warehouse.setLocation(location);

            warehouse.setOrders(order);
            warehouse.setPurchase(orderLink.getPurchase());
            warehouse.setOrderLink(orderLink);

            warehouses.add(warehouse);
        }

        // Lưu tất cả warehouse
        warehouses = warehouseRepository.saveAll(warehouses);
        ordersService.addProcessLog(order, shipmentCode, ProcessLogAction.DA_NHAP_KHO_NN);

        // Kiểm tra nếu tất cả orderLinks của order đã nhập kho
        List<OrderLinks> allOrderLinks = orderLinksRepository.findByOrdersOrderId(order.getOrderId());
        boolean allItemsReceived = allOrderLinks.stream()
                .allMatch(ol -> warehouseRepository.existsByOrderLinkLinkId(ol.getLinkId()));

        if (allItemsReceived) {
            order.setStatus(OrderStatus.CHO_THANH_TOAN_SHIP);
            ordersRepository.save(order);
        }

        return warehouses;
    }

    public Warehouse getWarehouseByTrackingCode(String trackingCode) {
        return warehouseRepository.findByTrackingCode(trackingCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mục kho với mã theo dõi: " + trackingCode));
    }

    public List<Warehouse> getWarehousesByOrderCode(String orderCode) {
        List<Warehouse> warehouses = warehouseRepository.findByOrdersOrderCode(orderCode);
        if (warehouses.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy mục kho nào cho đơn hàng: " + orderCode);
        }
        return warehouses;
    }

    public List<Warehouse> getWarehousesByPurchaseId(Long purchaseId) {
        List<Warehouse> warehouses = warehouseRepository.findByPurchasePurchaseId(purchaseId);
        if (warehouses.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy mục kho nào cho đơn mua hàng: " + purchaseId);
        }
        return warehouses;
    }

    public boolean isOrderLinkInWarehouse(Long orderLinkId) {
        return warehouseRepository.existsByOrderLinkLinkId(orderLinkId);
    }

    public boolean isPurchaseFullyReceived(Long purchaseId) {
        Purchases purchase = purchasesRepository.findById(purchaseId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn mua hàng này!"));
        List<OrderLinks> orderLinks = orderLinksRepository.findByPurchasePurchaseId(purchaseId);
        List<Warehouse> warehouses = warehouseRepository.findByPurchasePurchaseId(purchaseId);
        return !orderLinks.isEmpty() && orderLinks.size() == warehouses.size();
    }
}