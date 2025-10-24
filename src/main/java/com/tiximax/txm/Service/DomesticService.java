package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.*;
import com.tiximax.txm.Enums.*;
import com.tiximax.txm.Repository.DomesticRepository;
import com.tiximax.txm.Repository.OrderLinksRepository;
import com.tiximax.txm.Repository.OrdersRepository;
import com.tiximax.txm.Repository.PackingRepository;
import com.tiximax.txm.Utils.AccountUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service

public class DomesticService {

    @Autowired
    private DomesticRepository domesticRepository;

    @Autowired
    private PackingRepository packingRepository;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private OrdersService ordersService;

    @Autowired
    private OrderLinksRepository orderLinksRepository;

    @Autowired
    private AccountUtils accountUtils;

    public Domestic createDomesticForWarehousing(List<String> packingCodes, String note) {
    Staff staff = (Staff) accountUtils.getAccountCurrent();
    if (staff == null || staff.getWarehouseLocation() == null) {
        throw new IllegalArgumentException("Nhân viên hiện tại chưa được gán địa điểm kho!");
    }

    // 🔹 Tìm danh sách packings theo mã
    List<Packing> packings = packingRepository.findAllByPackingCodeIn(packingCodes);
    if (packings.isEmpty()) {
        throw new IllegalArgumentException("Không tìm thấy packing nào trong danh sách cung cấp!");
    }

    // 🔹 Kiểm tra trạng thái từng packing
    for (Packing packing : packings) {
        if (packing.getStatus() != PackingStatus.DA_BAY) {
            throw new IllegalArgumentException("Packing " + packing.getPackingCode() + " chưa đúng trạng thái nhập kho!");
        }
    }

    // 🔹 Lấy kho nước ngoài từ packing đầu tiên
    Packing firstPacking = packings.get(0);
    Set<Warehouse> warehouses = firstPacking.getWarehouses();
    if (warehouses.isEmpty()) {
        throw new IllegalArgumentException("Packing " + firstPacking.getPackingCode() + " không được liên kết với kho nước ngoài!");
    }

    Warehouse firstWarehouse = warehouses.iterator().next();
    WarehouseLocation fromLocation = firstWarehouse.getLocation();
    if (fromLocation == null) {
        throw new IllegalArgumentException("Kho nước ngoài của packing " + firstPacking.getPackingCode() + " không được tìm thấy!");
    }

    // 🔹 Cập nhật trạng thái OrderLinks
    List<String> shipmentCodes = packings.stream()
            .flatMap(p -> p.getPackingList().stream())
            .distinct()
            .collect(Collectors.toList());

    List<OrderLinks> orderLinks = orderLinksRepository.findByShipmentCodeIn(shipmentCodes);
    for (OrderLinks orderLink : orderLinks) {
        if (orderLink.getStatus() == OrderLinkStatus.DANG_CHUYEN_VN) {
            orderLink.setStatus(OrderLinkStatus.DA_NHAP_KHO_VN);
        }
    }
    orderLinksRepository.saveAll(orderLinks);
    updateOrderStatusIfAllLinksReady(orderLinks);

    for (Packing packing : packings) {
        packing.setStatus(PackingStatus.DA_NHAP_KHO_VN);
    }
    packingRepository.saveAll(packings);

    Domestic domestic = new Domestic();
    domestic.setFromLocation(fromLocation);
    domestic.setToLocation(staff.getWarehouseLocation());
    domestic.setStatus(DomesticStatus.NHAN_HANG);
    domestic.setTimestamp(LocalDateTime.now());
    domestic.setStaff(staff);
    domestic.setLocation(staff.getWarehouseLocation());
    domestic.setNote(note);
    Set<Packing> packingSet = new HashSet<>(packings);
    domestic.setPackings(packingSet);
    domestic.setShippingList(packings.stream()
            .map(Packing::getPackingCode)
            .collect(Collectors.toList()));
    domestic = domesticRepository.save(domestic);

    ordersService.addProcessLog(null, domestic.getDomesticId().toString(), ProcessLogAction.DA_NHAP_KHO_HN);

    return domestic;
}
    
    public List<Domestic>transferToCustomer() {
        List<Map<String, Object>> dataList = getReadyForDeliveryOrders(Pageable.unpaged());
        List<Domestic> results = new ArrayList<>();

        if (dataList == null || dataList.isEmpty()) return results;

        for (Map<String, Object> customerData : dataList) {
            String customerName = (String) customerData.get("customerName");
            List<Map<String, Object>> packings = (List<Map<String, Object>>) customerData.get("packings");

            if (packings == null || packings.isEmpty()) continue;

            List<String> shippingList = new ArrayList<>();
            Set<Packing> packingSet = new HashSet<>();

            for (Map<String, Object> packingData : packings) {
                String packingCode = (String) packingData.get("packingCode");
                Optional<Packing> optionalPacking = packingRepository.findByPackingCode(packingCode);
                if (optionalPacking.isPresent()) {
                    Packing packingEntity = optionalPacking.get();
                    packingSet.add(packingEntity);

                    Set<String> trackingCodes = (Set<String>) packingData.get("trackingCodes");
                    if (trackingCodes != null) shippingList.addAll(trackingCodes);
                }
            }

            Domestic domestic = new Domestic();
            domestic.setPackings(packingSet);
            domestic.setShippingList(shippingList);
            domestic.setStatus(DomesticStatus.DA_GIAO);
            domestic.setTimestamp(LocalDateTime.now());
            domestic.setNote("Giao hàng cho khách hàng: " + customerName);

        Staff currentStaff = (Staff) accountUtils.getAccountCurrent();
        domestic.setStaff(currentStaff);
        if (currentStaff != null) {
            domestic.setLocation(currentStaff.getWarehouseLocation());
            domestic.setFromLocation(currentStaff.getWarehouseLocation());
        } 
        domestic.setToLocation(null);
        results.add(domesticRepository.save(domestic));

            for (String shipmentCode : shippingList) {
            List<OrderLinks> links = orderLinksRepository.findByShipmentCode(shipmentCode);
            for (OrderLinks link : links) {
                link.setStatus(OrderLinkStatus.DA_GIAO); 
                orderLinksRepository.save(link);        
            }
        }
    }

        return results;
    }

    private void updateOrderStatusIfAllLinksReady(List<OrderLinks> orderLinks) {
        Map<Orders, List<OrderLinks>> orderToLinksMap = orderLinks.stream()
                .collect(Collectors.groupingBy(OrderLinks::getOrders));

        for (Map.Entry<Orders, List<OrderLinks>> entry : orderToLinksMap.entrySet()) {
            Orders order = entry.getKey();
//            List<OrderLinks> links = entry.getValue();
            Set<OrderLinks> allOrderLinks = order.getOrderLinks();

            boolean allLinksReady = allOrderLinks.stream()
                    .allMatch(link -> link.getStatus() == OrderLinkStatus.DA_NHAP_KHO_VN);

            if (allLinksReady) {
                order.setStatus(OrderStatus.DA_DU_HANG);
                ordersRepository.save(order);
            }
        }
    }

    public List<Map<String, Object>> getReadyForDeliveryOrders(Pageable pageable) {
        Page<Orders> ordersPage = ordersRepository.findByStatus(OrderStatus.CHO_GIAO, pageable);

        Map<Customer, List<Orders>> customerToOrdersMap = ordersPage.getContent().stream()
                .collect(Collectors.groupingBy(Orders::getCustomer));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Customer, List<Orders>> customerEntry : customerToOrdersMap.entrySet()) {
            Customer customer = customerEntry.getKey();
            Map<String, Object> customerData = new HashMap<>();
            customerData.put("customerName", customer.getName());
            customerData.put("customerPhone", customer.getPhone());
            customerData.put("customerAddress", customer.getAddress());

            Map<Packing, List<Warehouse>> packingToWarehousesMap = customerEntry.getValue().stream()
                    .flatMap(order -> order.getWarehouses().stream())
                    .filter(warehouse -> warehouse.getPacking() != null)
                    .collect(Collectors.groupingBy(Warehouse::getPacking));

            List<Map<String, Object>> packingsData = new ArrayList<>();
            for (Map.Entry<Packing, List<Warehouse>> packingEntry : packingToWarehousesMap.entrySet()) {
                Packing packing = packingEntry.getKey();
                Map<String, Object> packingData = new HashMap<>();
                packingData.put("packingCode", packing.getPackingCode());

//                Set<String> trackingCodes = packingEntry.getValue().stream()
//                        .map(Warehouse::getTrackingCode)
//                        .collect(Collectors.toSet());
                Set<String> trackingCodes = packingEntry.getValue().stream()
                        .filter(warehouse -> warehouse.getOrderLinks().stream()
                                .anyMatch(orderLink -> orderLink.getStatus() == OrderLinkStatus.CHO_GIAO))
                        .map(Warehouse::getTrackingCode)
                        .collect(Collectors.toSet());
                packingData.put("trackingCodes", trackingCodes);

                packingsData.add(packingData);
            }
            customerData.put("packings", packingsData);
            result.add(customerData);
        }

        return result;
    }

}
