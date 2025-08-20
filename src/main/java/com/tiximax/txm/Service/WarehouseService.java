package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.*;
import com.tiximax.txm.Enums.ProcessLogAction;
import com.tiximax.txm.Enums.WarehouseStatus;
import com.tiximax.txm.Model.WarehouseRequest;
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
    private OrdersService ordersService;

    @Autowired
    private AccountUtils accountUtils;

    public Warehouse createWarehouseEntry(Long purchaseId, Long locationId, WarehouseRequest warehouseRequest) {
        Purchases purchase = purchasesRepository.findById(purchaseId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn mua hàng với ID: " + purchaseId));

        Orders order = purchase.getOrders();
        if (order == null) {
            throw new IllegalArgumentException("Không tìm thấy đơn hàng liên quan đến đơn mua này!");
        }

        if (!ordersRepository.existsByOrderLinksLinkId(warehouseRequest.getOrderLinkId())) {
            throw new IllegalArgumentException("Không tồn tại mã đơn hàng: " + warehouseRequest.getOrderLinkId());
        }

        if (warehouseRepository.existsByPurchasePurchaseId(purchaseId)) {
            throw new IllegalArgumentException("Đã tồn tại mục kho cho đơn mua hàng với ID: " + purchaseId);
        }

        Double dim = (warehouseRequest.getLength() * warehouseRequest.getWidth() * warehouseRequest.getHeight()) / 6000;

        Warehouse warehouse = new Warehouse();
        warehouse.setTrackingCode(purchase.getTrackingNumber());
        warehouse.setLength(warehouseRequest.getLength());
        warehouse.setWidth(warehouseRequest.getWidth());
        warehouse.setHeight(warehouseRequest.getHeight());
        warehouse.setDim(dim);
        warehouse.setWeight(warehouseRequest.getWeight());
        warehouse.setNetWeight(warehouseRequest.getNetWeight());
        warehouse.setStatus(WarehouseStatus.DA_NHAP_KHO);
        warehouse.setCreatedAt(LocalDateTime.now());
//        warehouse.setStaff((Staff) accountUtils.getAccountCurrent());
        warehouse.setOrders(order);
        warehouse.setPurchase(purchase);

        WarehouseLocation location = new WarehouseLocation();
        location.setLocationId(locationId);
        warehouse.setLocation(location);

        warehouse.setPacking(null);

        warehouse = warehouseRepository.save(warehouse);

        ordersService.addProcessLog(order, ProcessLogAction.DA_NHAP_KHO_NN);

        return warehouse;
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

    public Warehouse getWarehouseByPurchaseId(Long purchaseId) {
        return warehouseRepository.findByPurchasePurchaseId(purchaseId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mục kho nào cho đơn mua hàng: " + purchaseId));
    }
}
