package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.*;
import com.tiximax.txm.Enums.OrderLinkStatus;
import com.tiximax.txm.Enums.OrderStatus;
import com.tiximax.txm.Enums.PackingStatus;
import com.tiximax.txm.Enums.ProcessLogAction;
import com.tiximax.txm.Model.PackingEligibleOrder;
import com.tiximax.txm.Model.PackingExport;
import com.tiximax.txm.Model.PackingInWarehouse;
import com.tiximax.txm.Model.PackingRequest;
import com.tiximax.txm.Repository.*;
import com.tiximax.txm.Utils.AccountUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PackingService {

    @Autowired
    private PackingRepository packingRepository;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private OrdersService ordersService;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private DestinationRepository destinationRepository;

    @Autowired
    private OrderLinksRepository orderLinksRepository;

    @Autowired
    private AccountUtils accountUtils;

    public Page<PackingEligibleOrder> getEligibleOrdersForPacking(Pageable pageable) {
        Staff staff = (Staff) accountUtils.getAccountCurrent();
        if (staff == null || staff.getWarehouseLocation() == null) {
            throw new IllegalArgumentException("Nhân viên hiện tại chưa được gán địa điểm kho!");
        }
        List<OrderStatus> statuses = Arrays.asList(OrderStatus.CHO_DONG_GOI, OrderStatus.DANG_XU_LY);
        Page<Orders> ordersPage = ordersRepository.findByStatusInAndWarehouses_Location_LocationId(
                statuses, staff.getWarehouseLocation().getLocationId(), pageable);

        List<PackingEligibleOrder> eligibleOrders = ordersPage.getContent().stream()
                .filter(order -> order.getWarehouses().stream()
                        .anyMatch(warehouse -> warehouse.getPacking() == null &&
                                warehouse.getOrderLinks().stream()
                                        .anyMatch(orderLink -> orderLink.getStatus() == OrderLinkStatus.DA_NHAP_KHO_NN)))
                .map(order -> {
                    Map<String, Integer> trackingToCount = new HashMap<>();
                    Set<Warehouse> warehouses = order.getWarehouses();
                    for (Warehouse warehouse : warehouses) {
                        if (warehouse.getPacking() == null) {
                            int productCount = (int) warehouse.getOrderLinks().stream()
                                    .filter(orderLink -> orderLink.getStatus() == OrderLinkStatus.DA_NHAP_KHO_NN)
                                    .count();
                            if (productCount > 0) {
                                trackingToCount.put(warehouse.getTrackingCode(), productCount);
                            }
                        }
                    }
                    PackingEligibleOrder eligibleOrder = new PackingEligibleOrder();
                    eligibleOrder.setOrderCode(order.getOrderCode());
                    eligibleOrder.setTrackingCodeToProductCount(trackingToCount);
                    return eligibleOrder;
                })
                .collect(Collectors.toList());

        return new PageImpl<>(eligibleOrders, pageable, ordersPage.getTotalElements());
    }

    public Packing createPacking(PackingRequest request) {
        Staff staff = (Staff) accountUtils.getAccountCurrent();
        if (staff.getWarehouseLocation() == null) {
            throw new IllegalArgumentException("Nhân viên hiện tại chưa được gán địa điểm kho!");
        }

        List<String> shipmentCodes = request.getShipmentCodes().stream()
                .distinct()
                .collect(Collectors.toList());
        if (shipmentCodes.isEmpty()) {
            throw new IllegalArgumentException("Danh sách mã vận đơn không được để trống!");
        }

        List<Warehouse> warehouses = warehouseRepository.findByTrackingCodeIn(shipmentCodes);
        if (warehouses.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy mã vận đơn bạn cung cấp!");
        }
        for (Warehouse warehouse : warehouses) {
        if (warehouse.getPacking() != null) {
            throw new IllegalArgumentException("Warehouse với mã vận đơn " + warehouse.getTrackingCode() + " đã có packingId, không thể tiếp tục.");
        }
        }


        Set<Orders> orders = warehouses.stream()
                .map(Warehouse::getOrders)
                .collect(Collectors.toSet());
        if (orders.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy đơn hàng nào liên quan đến các mã vận đơn bạn cung cấp!");
        }

        for (Orders order : orders) {
            if (!(order.getStatus().equals(OrderStatus.CHO_DONG_GOI) || order.getStatus().equals(OrderStatus.DANG_XU_LY))) {
                throw new IllegalArgumentException("Đơn hàng " + order.getOrderCode() + " chưa đủ điều kiện để đóng gói!");
            }
        }

        Destination destination = destinationRepository.findById(request.getDestinationId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy điểm đến này!"));
        for (Warehouse warehouse : warehouses) {
            if (!warehouse.getOrders().getDestination().getDestinationId().equals(destination.getDestinationId())) {
                throw new IllegalArgumentException("Các mã vận đơn phải có cùng điểm đến và trùng với điểm đến chung!");
            }
        }

        for (Warehouse warehouse : warehouses) {
            if (!warehouse.getOrders().getDestination().getDestinationId().equals(destination.getDestinationId())) {
                throw new IllegalArgumentException("Các mã vận đơn phải có cùng điểm đến!");
            }
            for (OrderLinks orderLink : warehouse.getOrderLinks()) {
                orderLink.setStatus(OrderLinkStatus.DA_DONG_GOI);
            }
        }

        List<String> packingList = orders.stream()
                .filter(Objects::nonNull)
                .flatMap(order -> order.getOrderLinks().stream())
                .filter(Objects::nonNull)
                .map(OrderLinks::getShipmentCode)
                .distinct()
                .collect(Collectors.toList());

        if (packingList.isEmpty()) {
            throw new IllegalArgumentException("Không có mã đơn hàng nào để tạo packing list!");
        }

        Packing packing = new Packing();
            String location = ((Staff) accountUtils.getAccountCurrent()).getWarehouseLocation().getName();
            packing.setPackingCode(generatePackingCode(location, destination.getDestinationName()));
            packing.setDestination(destination);
            packing.setPackingList(packingList);
            packing.setPackedDate(LocalDateTime.now());
            packing.setStaff(staff);
            packing.setStatus(PackingStatus.CHO_BAY);
            packing = packingRepository.save(packing);

        Set<Warehouse> packingWarehouses = new HashSet<>(warehouses);
        packing.setWarehouses(packingWarehouses);

        for (Warehouse warehouse : warehouses) {
            warehouse.setPacking(packing);
            warehouseRepository.save(warehouse);
        }

        for (Orders order : orders) {
            order.setStatus(OrderStatus.DANG_XU_LY);
        }
        ordersService.addProcessLog(null, packing.getPackingCode(), ProcessLogAction.DA_DONG_GOI);

        return packing;
    }

    public Page<PackingInWarehouse> getPackingsInWarehouse(Pageable pageable) {
        Staff staff = (Staff) accountUtils.getAccountCurrent();
        if (staff == null || staff.getWarehouseLocation() == null) {
            throw new IllegalArgumentException("Nhân viên hiện tại chưa được gán địa điểm kho!");
        }

        Page<Packing> packingsPage = packingRepository.findByFlightCodeIsNullAndWarehouses_Location_LocationId(staff.getWarehouseLocation().getLocationId(), pageable);

        return packingsPage.map(packing -> {
            Map<String, Integer> trackingToCount = new HashMap<>();
            Set<Warehouse> warehouses = packing.getWarehouses();
            for (Warehouse warehouse : warehouses) {
                String trackingCode = warehouse.getTrackingCode();
                int productCount = warehouse.getOrderLinks().size();
                trackingToCount.put(trackingCode, productCount);
            }

            PackingInWarehouse piw = new PackingInWarehouse();
            piw.setPackingCode(packing.getPackingCode());
            piw.setPackedDate(packing.getPackedDate());
            piw.setTrackingCodeToProductCount(trackingToCount);
            return piw;
        });
    }

    public Packing getPackingById(Long id) {
    return packingRepository.findById(id).orElse(null);
}
    public Page<Packing> getPackingsAwaitingFlight(Pageable pageable) {
        Staff staff = (Staff) accountUtils.getAccountCurrent();
        if (staff == null || staff.getWarehouseLocation() == null) {
            throw new IllegalArgumentException("Nhân viên hiện tại chưa được gán địa điểm kho!");
        }
        return packingRepository.findByFlightCodeIsNullAndWarehouses_Location_LocationId(staff.getWarehouseLocation().getLocationId(), pageable);
    }

    public void assignFlightCode(List<Long> packingIds, String flightCode) {
        List<Packing> packings = packingRepository.findAllById(packingIds)
                .stream()
                .filter(packing -> packing.getFlightCode() == null)
                .peek(packing -> packing.setFlightCode(flightCode))
                .collect(Collectors.toList());

        if (packings.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy packing nào phù hợp hoặc đã được gán chuyến bay!");
        }

        for (Packing packing : packings) {
            packing.setStatus(PackingStatus.DA_BAY);
            packingRepository.save(packing);

            List<String> shipmentCodes = packing.getPackingList();
            List<OrderLinks> orderLinks = orderLinksRepository.findByShipmentCodeIn(shipmentCodes);

            for (OrderLinks orderLink : orderLinks) {
                orderLink.setStatus(OrderLinkStatus.DANG_CHUYEN_VN);
            }
            orderLinksRepository.saveAll(orderLinks);

            ordersService.addProcessLog(null, packing.getPackingCode(), ProcessLogAction.DA_BAY);
        }
    }

    private String generatePackingCode(String location, String destinationName) {
        String monthYear = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMyy"));
        String[] words = destinationName.split("\\s+");
        String shortDestination = "";
        for (String word : words) {
            if (!word.isEmpty()) {
                shortDestination += word.charAt(0);
            }
        }
        String baseCode = location + monthYear + shortDestination;

        String finalCode = baseCode + "1";
        boolean exists;
        int sequence = 1 ;

          do {
        exists = packingRepository.existsByPackingCode(finalCode);
        if (exists) {
            sequence++;
            finalCode = baseCode + sequence;
        }
        } while (exists);

        return finalCode;
    }

    private int getNextSequence(String baseCode) {
        List<Packing> existingPackings = packingRepository.findByPackingCodeStartingWith(baseCode);
        if (existingPackings.isEmpty()) {
            return 1;
        }
        Packing lastPacking = existingPackings.get(existingPackings.size() - 1);
        String lastCode = lastPacking.getPackingCode();
        if (lastCode != null && lastCode.startsWith(baseCode)) {
            try {
                String seqStr = lastCode.substring(baseCode.length());
                int seq = Integer.parseInt(seqStr);
                return seq + 1;
            } catch (NumberFormatException e) {
                return 1;
            }
        }
        return 1;
    }

    public Page<Packing> getPackingsWithFlightStatus(Pageable pageable) {
        Page<Packing> packings = packingRepository.findByStatus(PackingStatus.DA_BAY, pageable);
        packings.getContent().forEach(packing -> {
            List<String> filteredList = packing.getPackingList().stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            packing.setPackingList(filteredList);
        });
        return packings;
    }

    public Packing removeShipmentFromPacking(String packingCode, List<String> shipmentCodesToRemove) {
        Packing packing = packingRepository.findByPackingCode(packingCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy " + packingCode));

        if (packing.getStatus() == PackingStatus.DA_BAY) {
            throw new IllegalArgumentException("Không thể gỡ hàng khỏi packing đã bay!");
        }

        List<Warehouse> warehousesToRemove = warehouseRepository.findByPackingPackingIdAndTrackingCodeIn(
                packing.getPackingId(), shipmentCodesToRemove);

        if (warehousesToRemove.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy mã vận đơn nào để gỡ!");
        }

        List<String> currentPackingList = new ArrayList<>(packing.getPackingList());
        currentPackingList.removeAll(shipmentCodesToRemove);
        packing.setPackingList(currentPackingList);

        Set<Orders> affectedOrders = new HashSet<>();

        for (Warehouse warehouse : warehousesToRemove) {
            warehouse.setPacking(null);
            for (OrderLinks orderLink : warehouse.getOrderLinks()) {
                orderLink.setStatus(OrderLinkStatus.DA_NHAP_KHO_NN);
            }

            affectedOrders.add(warehouse.getOrders());
            warehouseRepository.save(warehouse);
        }
        for (Orders order : affectedOrders) {
            boolean hasPackedItems = order.getWarehouses().stream()
                    .anyMatch(w -> w.getPacking() != null);

            if (!hasPackedItems) {
                order.setStatus(OrderStatus.DANG_XU_LY);
            }
            ordersRepository.save(order);
        }

        if (currentPackingList.isEmpty()) {
            packingRepository.delete(packing);
            return null;
        }

        packingRepository.save(packing);

        ordersService.addProcessLog(
                null,
                packing.getPackingCode(),
                ProcessLogAction.DA_CHINH_SUA
        );

        return packing;
    }

    public List<String> getPackingListByCode(String packingCode) {
        List<String> list = packingRepository.findPackingListByCode(packingCode);
        if (list == null || list.isEmpty()) {
            throw new RuntimeException("Không tìm thấy packingList cho mã: " + packingCode);
        }
        return list;
    }

    public List<PackingExport> getPackingExportByIdsChoBay(List<Long> packingIds) {

        List<Packing> packings = packingRepository.findAllChoBayWithWarehouses(packingIds);

    List<PackingExport> exports = new ArrayList<>();

    for (Packing packing : packings) {
        for (Warehouse w : packing.getWarehouses()) {

            PackingExport dto = new PackingExport();

            dto.setPackingCode(packing.getPackingCode());

            dto.setTrackingCode(w.getTrackingCode());
            dto.setHeight(w.getHeight());
            dto.setLength(w.getLength());
            dto.setWeight(w.getWeight());
            dto.setWidth(w.getWidth());
            dto.setDim(w.getDim());
            dto.setNetWeight(w.getNetWeight());

            // 🔥 LẤY PRODUCT NAME
            List<OrderLinks> links = orderLinksRepository.findByWarehouse(w);
            if (!links.isEmpty()) {
                dto.setProductName(links.get(0).getProductName()); 
                // hoặc join tất cả productName nếu muốn
            }

            // ORDER INFO
            Orders order = w.getOrders();
            if (order != null) {
                dto.setOrderCode(order.getOrderCode());
                dto.setDestination(order.getDestination().getDestinationName());

                if (order.getCustomer() != null) {
                    dto.setStaffName(order.getStaff().getName());
                    dto.setCustomerCode(order.getCustomer().getCustomerCode());
                    dto.setCustomerName(order.getCustomer().getName());
                }
            }

            exports.add(dto);
        }
    }

    return exports;
}

  
}