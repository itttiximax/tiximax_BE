package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.*;
import com.tiximax.txm.Enums.OrderLinkStatus;
import com.tiximax.txm.Enums.OrderStatus;
import com.tiximax.txm.Enums.ProcessLogAction;
import com.tiximax.txm.Enums.WarehouseStatus;
import com.tiximax.txm.Model.WarehouseRequest;
import com.tiximax.txm.Model.WarehouseSummary;
import com.tiximax.txm.Repository.OrderLinksRepository;
import com.tiximax.txm.Repository.OrdersRepository;
import com.tiximax.txm.Repository.PurchasesRepository;
import com.tiximax.txm.Repository.WarehouseRepository;
import com.tiximax.txm.Utils.AccountUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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

//    public Warehouse createWarehouseEntryByShipmentCode(String shipmentCode, WarehouseRequest warehouseRequest) {
//        List<OrderLinks> orderLinks = orderLinksRepository.findByShipmentCode(shipmentCode);
//        if (orderLinks.isEmpty()) {
//            throw new IllegalArgumentException("Không tìm thấy mã sản phẩm với mã vận đơn: " + shipmentCode + "!");
//        }
//
//        Orders order = orderLinks.get(0).getOrders();
//        if (order == null) {
//            throw new IllegalArgumentException("Không tìm thấy đơn hàng liên quan đến mã vận đơn này!");
//        }
//
//        if (!order.getStatus().equals(OrderStatus.CHO_NHAP_KHO_NN)) {
//            throw new RuntimeException("Đơn hàng chưa đủ điều kiện để nhập kho!");
//        }
//
//        List<WarehouseRequest.ProductDetail> productDetails = warehouseRequest.getProducts();
//        if (productDetails.size() != 1) {
//            throw new IllegalArgumentException("Chỉ cần một ProductDetail chứa thông tin tổng cho kiện hàng!");
//        }
//
//        if (warehouseRepository.existsByTrackingCode(shipmentCode)) {
//            throw new IllegalArgumentException("Mục kho đã tồn tại cho mã vận đơn: " + shipmentCode);
//        }
//
//        WarehouseRequest.ProductDetail productDetail = productDetails.get(0);
//        Double dim = (productDetail.getLength() * productDetail.getWidth() * productDetail.getHeight()) / 5000;
//
//        Staff staff = (Staff) accountUtils.getAccountCurrent();
//        if (staff.getWarehouseLocation() == null) {
//            throw new IllegalArgumentException("Nhân viên hiện tại chưa được gán địa điểm kho!");
//        }
//        WarehouseLocation location = new WarehouseLocation();
//        location.setLocationId(staff.getWarehouseLocation().getLocationId());
//
//        Warehouse warehouse = new Warehouse();
//        warehouse.setTrackingCode(shipmentCode);
//        warehouse.setLength(productDetail.getLength());
//        warehouse.setWidth(productDetail.getWidth());
//        warehouse.setHeight(productDetail.getHeight());
//        warehouse.setDim(dim);
//        warehouse.setWeight(productDetail.getWeight());
//        warehouse.setNetWeight(productDetail.getNetWeight());
//        warehouse.setImage(productDetail.getImage());
//        warehouse.setStatus(WarehouseStatus.DA_NHAP_KHO);
//        warehouse.setCreatedAt(LocalDateTime.now());
//        warehouse.setStaff(staff);
//        warehouse.setLocation(location);
//        warehouse.setOrders(order);
//        warehouse.setPurchase(orderLinks.get(0).getPurchase());
//        warehouse.setOrderLinks(new HashSet<>(orderLinks));
//        warehouse.setPacking(null);
//
//        orderLinks.forEach(orderLink -> {
//            orderLink.setWarehouse(warehouse);
//            orderLink.setStatus(OrderLinkStatus.DA_NHAP_KHO);
//        });
//
//        warehouseRepository.save(warehouse);
//        orderLinksRepository.saveAll(orderLinks);
//
//        ordersService.addProcessLog(order, shipmentCode, ProcessLogAction.DA_NHAP_KHO_NN);
//
//        List<OrderLinks> allOrderLinks = orderLinksRepository.findByOrdersOrderId(order.getOrderId());
//        boolean allItemsReceived = allOrderLinks.stream()
//                .allMatch(ol -> ol.getWarehouse() != null);
//
//        if (allItemsReceived) {
//            order.setStatus(OrderStatus.CHO_DONG_GOI);
//            ordersRepository.save(order);
//            ordersService.addProcessLog(order, order.getOrderCode(), ProcessLogAction.DA_NHAP_KHO_NN);
//        }
//
//        return warehouse;
//    }

    public Warehouse createWarehouseEntryByShipmentCode(String shipmentCode, WarehouseRequest warehouseRequest) {
        List<OrderLinks> orderLinks = orderLinksRepository.findByShipmentCode(shipmentCode);
        if (orderLinks.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy mã sản phẩm với mã vận đơn " + shipmentCode + "!");
        }

        Orders order = orderLinks.get(0).getOrders();
        if (order == null) {
            throw new IllegalArgumentException("Không tìm thấy đơn hàng liên quan đến mã vận đơn này!");
        }

        if (!order.getStatus().equals(OrderStatus.CHO_NHAP_KHO_NN)) {
            throw new RuntimeException("Đơn hàng chưa đủ điều kiện để nhập kho!");
        }

        if (warehouseRepository.existsByTrackingCode(shipmentCode)) {
            throw new IllegalArgumentException("Mục kho đã tồn tại cho mã vận đơn này!");
        }

        Double dim = (warehouseRequest.getLength() * warehouseRequest.getWidth() * warehouseRequest.getHeight()) / 5000;

        Staff staff = (Staff) accountUtils.getAccountCurrent();
        if (staff.getWarehouseLocation() == null) {
            throw new IllegalArgumentException("Nhân viên hiện tại chưa được gán địa điểm kho!");
        }
        WarehouseLocation location = new WarehouseLocation();
        location.setLocationId(staff.getWarehouseLocation().getLocationId());

        Warehouse warehouse = new Warehouse();
        warehouse.setTrackingCode(shipmentCode);
        warehouse.setLength(warehouseRequest.getLength());
        warehouse.setWidth(warehouseRequest.getWidth());
        warehouse.setHeight(warehouseRequest.getHeight());
        warehouse.setDim(dim);
        warehouse.setWeight(warehouseRequest.getWeight());
        warehouse.setNetWeight(warehouseRequest.getNetWeight());
        warehouse.setImage(warehouseRequest.getImage());
        warehouse.setStatus(WarehouseStatus.DA_NHAP_KHO);
        warehouse.setCreatedAt(LocalDateTime.now());
        warehouse.setStaff(staff);
        warehouse.setLocation(location);
        warehouse.setOrders(order);
        warehouse.setPurchase(orderLinks.get(0).getPurchase());
        warehouse.setOrderLinks(new HashSet<>(orderLinks));
        warehouse.setPacking(null);

        orderLinks.forEach(orderLink -> {
            orderLink.setWarehouse(warehouse);
            orderLink.setStatus(OrderLinkStatus.DA_NHAP_KHO);
        });

        warehouseRepository.save(warehouse);
        orderLinksRepository.saveAll(orderLinks);

        ordersService.addProcessLog(order, shipmentCode, ProcessLogAction.DA_NHAP_KHO_NN);

        List<OrderLinks> allOrderLinks = orderLinksRepository.findByOrdersOrderId(order.getOrderId());
        boolean allItemsReceived = allOrderLinks.stream()
                .allMatch(ol -> ol.getWarehouse() != null);

        if (allItemsReceived) {
            order.setStatus(OrderStatus.CHO_DONG_GOI);
            ordersRepository.save(order);
            ordersService.addProcessLog(order, order.getOrderCode(), ProcessLogAction.DA_NHAP_KHO_NN);
        }

        return warehouse;
    }

    public Page<WarehouseSummary> getWarehousesForPacking(Pageable pageable) {
        Page<Warehouse> warehousePage = warehouseRepository.findByStatus(WarehouseStatus.DA_NHAP_KHO, pageable);
        List<WarehouseSummary> summaries = warehousePage.getContent().stream()
                .map(w -> new WarehouseSummary(
                        w.getWarehouseId(),
                        w.getTrackingCode(),
                        w.getOrders().getOrderCode(),
                        w.getWeight(),
                        w.getNetWeight(),
                        w.getDim(),
                        w.getCreatedAt()
                ))
                .collect(Collectors.toList());
        return new PageImpl<>(summaries, pageable, warehousePage.getTotalElements());
    }

    public Map<String, Double> calculateWarehouseTotals() {
        List<Warehouse> warehouses = warehouseRepository.findAllByStatus(WarehouseStatus.DA_NHAP_KHO);
        Map<String, Double> totals = new HashMap<>();
        totals.put("totalWeight", warehouses.stream().mapToDouble(Warehouse::getWeight).sum());
        totals.put("totalNetWeight", warehouses.stream().mapToDouble(Warehouse::getNetWeight).sum());
        totals.put("totalDim", warehouses.stream().mapToDouble(Warehouse::getDim).sum());
        return totals;
    }

}