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

    public List<Warehouse> createWarehouseEntry(Long purchaseId, Long locationId, WarehouseRequest warehouseRequest) {
        Purchases purchase = purchasesRepository.findById(purchaseId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn mua hàng này!"));

        Orders order = purchase.getOrders();
        if (order == null) {
            throw new IllegalArgumentException("Không tìm thấy đơn hàng liên quan đến đơn mua này!");
        }

        if (!order.getStatus().equals(OrderStatus.CHO_NHAP_KHO_NN)){
            throw new RuntimeException("Đơn hàng chưa đủ điều kiện để nhập kho!");
        }

        List<OrderLinks> orderLinks = orderLinksRepository.findByPurchasePurchaseId(purchaseId);
        if (orderLinks.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy mã sản phẩm cho đơn mua hàng này!");
        }

        List<WarehouseRequest.ProductDetail> productDetails = warehouseRequest.getProducts();
        if (productDetails.size() != orderLinks.size()) {
            throw new IllegalArgumentException("Số lượng sản phẩm trong yêu cầu kho (" + productDetails.size() +
                    ") không khớp với số lượng sản phẩm thực (" + orderLinks.size() + ")!");
        }

        Object currentAccount = accountUtils.getAccountCurrent();
        if (!(currentAccount instanceof Staff)) {
            throw new IllegalStateException("Chỉ nhân viên được phép tạo mục kho.");
        }
        Staff staff = (Staff) currentAccount;

        List<Warehouse> warehouses = new ArrayList<>();
        for (int i = 0; i < orderLinks.size(); i++) {
            OrderLinks orderLink = orderLinks.get(i);
            WarehouseRequest.ProductDetail productDetail = productDetails.get(i);

            if (!orderLink.getLinkId().equals(productDetail.getOrderLinkId())) {
                throw new IllegalArgumentException("Mã sản phẩm " + productDetail.getOrderLinkId() +
                        " không khớp với sản phẩm tại vị trí " + i);
            }

            if (warehouseRepository.existsByPurchasePurchaseIdAndOrderLinkLinkId(purchaseId, orderLink.getLinkId())) {
                throw new IllegalArgumentException("Mục kho đã tồn tại cho sản phẩm với mã: " + orderLink.getTrackingCode());
            }

            Double dim = (productDetail.getLength() * productDetail.getWidth() * productDetail.getHeight()) / 5000;

            Warehouse warehouse = new Warehouse();
            warehouse.setTrackingCode(orderLink.getTrackingCode());
            warehouse.setLength(productDetail.getLength());
            warehouse.setWidth(productDetail.getWidth());
            warehouse.setHeight(productDetail.getHeight());
            warehouse.setDim(dim);
            warehouse.setWeight(productDetail.getWeight());
            warehouse.setNetWeight(productDetail.getNetWeight());
            warehouse.setStatus(WarehouseStatus.DA_NHAP_KHO);
            warehouse.setCreatedAt(LocalDateTime.now());
            warehouse.setStaff(staff);
            warehouse.setOrders(order);
            warehouse.setPurchase(purchase);
            warehouse.setOrderLink(orderLink);
            warehouse.setPacking(null);

            WarehouseLocation location = new WarehouseLocation();
            location.setLocationId(locationId);
            warehouse.setLocation(location);

            warehouses.add(warehouse);
            ordersService.addProcessLog(order, purchase.getPurchaseCode(), ProcessLogAction.DA_NHAP_KHO_NN);
        }

        warehouses = warehouseRepository.saveAll(warehouses);

        List<OrderLinks> allOrderLinks = orderLinksRepository.findByOrdersOrderId(order.getOrderId());
        boolean allItemsReceived = allOrderLinks.stream()
                .allMatch(orderLink -> warehouseRepository.existsByOrderLinkLinkId(orderLink.getLinkId()));

        if (allItemsReceived) {
            order.setStatus(OrderStatus.CHO_DONG_GOI);
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