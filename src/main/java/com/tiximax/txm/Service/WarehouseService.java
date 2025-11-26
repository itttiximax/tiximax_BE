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
import org.hibernate.query.Order;
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
    private OrdersRepository ordersRepository;

    @Autowired
    private OrderLinksRepository orderLinksRepository;

    @Autowired
    private OrdersService ordersService;

    @Autowired
    private AccountUtils accountUtils;

    public Warehouse createWarehouseEntryByShipmentCode(String shipmentCode, WarehouseRequest warehouseRequest) {
        List<OrderLinks> orderLinks = orderLinksRepository.findByShipmentCode(shipmentCode);
        if (orderLinks.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy mã sản phẩm với mã vận đơn " + shipmentCode + "!");
        }

        Orders order = orderLinks.get(0).getOrders();
        if (order == null) {
            throw new IllegalArgumentException("Không tìm thấy đơn hàng liên quan đến mã vận đơn này!");
        }

        if (!(order.getStatus().equals(OrderStatus.CHO_NHAP_KHO_NN) ||
                order.getStatus().equals(OrderStatus.CHO_DONG_GOI) ||
                order.getStatus().equals(OrderStatus.DANG_XU_LY))) {
            throw new RuntimeException("Đơn hàng chưa đủ điều kiện để nhập kho!");
        }

        if (order.getCheckRequired() && warehouseRequest.getImageCheck().isEmpty()){
            throw new RuntimeException("Đơn hàng này cần được kiểm tra trước khi nhập kho!");
        }

        if (warehouseRepository.existsByTrackingCode(shipmentCode)) {
            throw new IllegalArgumentException("Mục kho đã tồn tại cho mã vận đơn này!");
        }

        Staff staff = (Staff) accountUtils.getAccountCurrent();
        if (staff.getWarehouseLocation() == null) {
            throw new IllegalArgumentException("Nhân viên hiện tại chưa được gán địa điểm kho!");
        }

        WarehouseLocation location = new WarehouseLocation();
        location.setLocationId(staff.getWarehouseLocation().getLocationId());

        if (order.getStatus().equals(OrderStatus.CHO_NHAP_KHO_NN)) {
            order.setStatus(OrderStatus.CHO_DONG_GOI);
            ordersRepository.save(order);
        }

        Double dim = (warehouseRequest.getLength() * warehouseRequest.getWidth() * warehouseRequest.getHeight()) / 5000;

        Warehouse warehouse = new Warehouse();
            warehouse.setTrackingCode(shipmentCode);
            warehouse.setLength(warehouseRequest.getLength());
            warehouse.setWidth(warehouseRequest.getWidth());
            warehouse.setHeight(warehouseRequest.getHeight());
            warehouse.setDim(dim);
            warehouse.setWeight(warehouseRequest.getWeight());
            if (dim > warehouse.getWeight()){
                warehouse.setNetWeight(dim);
            } else {
                warehouse.setNetWeight(warehouse.getWeight());
            }
            warehouse.setImage(warehouseRequest.getImage());
            warehouse.setImageCheck(warehouseRequest.getImageCheck());  
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
            orderLink.setStatus(OrderLinkStatus.DA_NHAP_KHO_NN);
        });

        warehouseRepository.save(warehouse);
        orderLinksRepository.saveAll(orderLinks);

        ordersService.addProcessLog(order, shipmentCode, ProcessLogAction.DA_NHAP_KHO_NN);

        return warehouse;
    }

    public Optional<Warehouse> getWarehouseById(Long id) {
    return warehouseRepository.findById(id);
}

    public String createWarehouseEntryByListShipmentCodes(List<String> shipmentCodes) {
        Staff staff = (Staff) accountUtils.getAccountCurrent();
        if (staff.getWarehouseLocation() == null) {
            throw new IllegalArgumentException("Nhân viên hiện tại chưa được gán địa điểm kho!");
        }

        WarehouseLocation location = new WarehouseLocation();
        location.setLocationId(staff.getWarehouseLocation().getLocationId());

        for (String shipmentCode : shipmentCodes) {
            List<OrderLinks> orderLinks = orderLinksRepository.findByShipmentCode(shipmentCode);
            if (orderLinks.isEmpty()) {
                throw new IllegalArgumentException("Không tìm thấy mã sản phẩm với mã vận đơn " + shipmentCode + "!");
            }

            Orders order = orderLinks.get(0).getOrders();
            if (order == null) {
                throw new IllegalArgumentException("Không tìm thấy đơn hàng liên quan đến mã vận đơn này!");
            }

            if (!(order.getStatus().equals(OrderStatus.CHO_NHAP_KHO_NN) ||
                    order.getStatus().equals(OrderStatus.CHO_DONG_GOI) ||
                    order.getStatus().equals(OrderStatus.DANG_XU_LY))) {
                throw new RuntimeException("Đơn hàng chưa đủ điều kiện để nhập kho!");
            }

            if (warehouseRepository.existsByTrackingCode(shipmentCode)) {
                throw new IllegalArgumentException("Mục kho đã tồn tại cho mã vận đơn này!");
            }

            if (order.getStatus().equals(OrderStatus.CHO_NHAP_KHO_NN)) {
                order.setStatus(OrderStatus.CHO_DONG_GOI);
                ordersRepository.save(order);
            }

            Warehouse warehouse = new Warehouse();
                warehouse.setTrackingCode(shipmentCode);
                warehouse.setLength(null);
                warehouse.setWidth(null);
                warehouse.setHeight(null);
                warehouse.setDim(null);
                warehouse.setWeight(null);
                warehouse.setNetWeight(null);
                warehouse.setImage(null);
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
                orderLink.setStatus(OrderLinkStatus.DA_NHAP_KHO_NN);
            });

            warehouseRepository.save(warehouse);
            orderLinksRepository.saveAll(orderLinks);

            ordersService.addProcessLog(order, shipmentCode, ProcessLogAction.DA_NHAP_KHO_NN);
        }

        return "Thành công nhập kho các mã vận đơn vừa truyền vào!";
    }

    public Page<WarehouseSummary> getWarehousesForPacking(Pageable pageable) {

        Staff staff = (Staff) accountUtils.getAccountCurrent();
        if (staff.getWarehouseLocation() == null) {
            throw new IllegalArgumentException("Nhân viên hiện tại chưa được gán địa điểm kho!");
        }
         WarehouseLocation location = new WarehouseLocation();
        location.setLocationId(staff.getWarehouseLocation().getLocationId());

    Page<Warehouse> warehousePage =
            warehouseRepository.findByStatusAndLocation_LocationId(
                    WarehouseStatus.DA_NHAP_KHO,
                    location.getLocationId(),
                    pageable
            );        List<WarehouseSummary> summaries = warehousePage.getContent().stream()
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

    public Warehouse updateWarehouseNetWeight(String trackingCode, WarehouseRequest request) {
        Optional<Warehouse> warehouseOptional = warehouseRepository.findByTrackingCode(trackingCode);
        if (warehouseOptional.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy kho với mã tracking: " + trackingCode);
        }
        Warehouse warehouse = warehouseOptional.get();

        Staff staff = (Staff) accountUtils.getAccountCurrent();
        if (staff.getWarehouseLocation() == null) {
            throw new IllegalArgumentException("Nhân viên hiện tại chưa được gán địa điểm kho!");
        }

        if (warehouse.getNetWeight() != null) {
            throw new IllegalArgumentException("Kho này đã có số cân, không thể cập nhật!");
        }

        if (request.getLength() == null || request.getWidth() == null || request.getHeight() == null || request.getWeight() == null) {
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ kích thước và cân nặng!");
        }

        Double dim = (request.getLength() * request.getWidth() * request.getHeight()) / 6000;
        warehouse.setLength(request.getLength());
        warehouse.setWidth(request.getWidth());
        warehouse.setHeight(request.getHeight());
        warehouse.setDim(dim);
        warehouse.setWeight(request.getWeight());
        warehouse.setNetWeight(dim > request.getWeight() ? dim : request.getWeight());
        warehouse.setImage(request.getImage());

        return warehouseRepository.save(warehouse);
    }

    public boolean hasNetWeight(String trackingCode) {
        return warehouseRepository.findByTrackingCode(trackingCode)
                .map(w -> w.getNetWeight() != null)
                .orElse(false);
    }

    public List<String> suggestShipmentCodes(String keyword) {
        return orderLinksRepository.suggestShipmentCodes(keyword);
    }
}