package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.*;
import com.tiximax.txm.Enums.*;
import com.tiximax.txm.Model.DomesticResponse;
import com.tiximax.txm.Repository.AddressRepository;
import com.tiximax.txm.Repository.DomesticRepository;
import com.tiximax.txm.Repository.OrderLinksRepository;
import com.tiximax.txm.Repository.OrdersRepository;
import com.tiximax.txm.Repository.PackingRepository;
import com.tiximax.txm.Repository.PartialShipmentRepository;
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
    private PartialShipmentService partialShipmentService;
    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private PartialShipmentRepository partialShipmentRepository;

    @Autowired
    private AddressRepository addressRepository; 
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

    List<Packing> packings = packingRepository.findAllByPackingCodeIn(packingCodes);
    if (packings.isEmpty()) {
        throw new IllegalArgumentException("Không tìm thấy packing nào trong danh sách cung cấp!");
    }

    for (Packing packing : packings) {
        if (packing.getStatus() != PackingStatus.DA_BAY) {
            throw new IllegalArgumentException("Packing " + packing.getPackingCode() + " chưa đúng trạng thái nhập kho!");
        }
    }

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
    
 public List<DomesticResponse> TransferToCustomer() {
    List<Map<String, Object>> dataList = getReadyForDeliveryOrders(Pageable.unpaged());
    List<Domestic> domesticList = new ArrayList<>();

    if (dataList == null || dataList.isEmpty()) return Collections.emptyList();

    Staff currentStaff = (Staff) accountUtils.getAccountCurrent();
    WarehouseLocation currentLocation = currentStaff != null ? currentStaff.getWarehouseLocation() : null;

    for (Map<String, Object> customerData : dataList) {
        String customerName = (String) customerData.get("customerName");
        List<Map<String, Object>> addresses = (List<Map<String, Object>>) customerData.get("addresses");
        if (addresses == null || addresses.isEmpty()) continue;

        for (Map<String, Object> addressData : addresses) {
            Long addressId = ((Number) addressData.get("addressId")).longValue();

            Address address = addressRepository.findById(addressId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ ID: " + addressId));

            System.out.println("📦 Đang tạo Domestic cho địa chỉ: " + address.getAddressName());

            List<Map<String, Object>> packings = (List<Map<String, Object>>) addressData.get("packings");
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
            if (packingSet.isEmpty()) continue;

            Domestic domestic = new Domestic();
            domestic.setPackings(packingSet);
            domestic.setShippingList(shippingList);
            domestic.setStatus(DomesticStatus.DA_GIAO);
            domestic.setTimestamp(LocalDateTime.now());
            domestic.setNote("Giao hàng cho khách hàng: " + customerName);
            domestic.setToAddress(address);
            if (currentStaff != null) {
                domestic.setStaff(currentStaff);
                domestic.setFromLocation(currentLocation);
                domestic.setLocation(currentLocation);
            }
            domestic.setToLocation(null);

            domestic = domesticRepository.save(domestic);
            domesticList.add(domestic);

           for (String shipmentCode : shippingList) {
    List<OrderLinks> links = orderLinksRepository.findByShipmentCode(shipmentCode);
        
        for (OrderLinks link : links) {
            link.setStatus(OrderLinkStatus.DA_GIAO);
        }
        orderLinksRepository.saveAll(links);

        Set<Orders> relatedOrders = links.stream()
                .map(OrderLinks::getOrders)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<PartialShipment> relatedPartials = links.stream()
                .map(OrderLinks::getPartialShipment)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (Orders order : relatedOrders) {
            Set<OrderLinks> allLinks = order.getOrderLinks();

            boolean allDeliveredOrCanceled = allLinks.stream().allMatch(l ->
                    l.getStatus() == OrderLinkStatus.DA_GIAO ||
                    l.getStatus() == OrderLinkStatus.DA_HUY
            );

            if (allDeliveredOrCanceled && order.getStatus() != OrderStatus.DA_GIAO) {
                order.setStatus(OrderStatus.DA_GIAO);
                ordersRepository.save(order);
                ordersService.addProcessLog(
                        order,
                        domestic.getDomesticId().toString(),
                        ProcessLogAction.DA_GIAO
                );
            }
        }

        for (PartialShipment partial : relatedPartials) {
            Set<OrderLinks> partialLinks = partial.getReadyLinks();

            boolean allLinksDeliveredOrCanceled = partialLinks.stream().allMatch(link ->
                    link.getStatus() == OrderLinkStatus.DA_GIAO ||
                    link.getStatus() == OrderLinkStatus.DA_HUY
            );

            if (allLinksDeliveredOrCanceled && partial.getStatus() != OrderStatus.DA_GIAO) {
                partial.setStatus(OrderStatus.DA_GIAO);
                partialShipmentRepository.save(partial); 

                ordersService.addProcessLog(
                        partial.getOrders(), 
                        "PartialShipment#" + partial.getId(),
                        ProcessLogAction.DA_GIAO
                );
            }
                }
            }
        }
    }

    return domesticList.stream()
            .map(DomesticResponse::fromEntity)
            .collect(Collectors.toList());
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
    List<OrderStatus> statuses = Arrays.asList(OrderStatus.CHO_GIAO, OrderStatus.DANG_XU_LY);
    Page<Orders> ordersPage = ordersRepository.findByStatuses(statuses, pageable);

    Map<Customer, List<Orders>> customerToOrdersMap = ordersPage.getContent().stream()
            .collect(Collectors.groupingBy(Orders::getCustomer));

    List<Map<String, Object>> result = new ArrayList<>();

    for (Map.Entry<Customer, List<Orders>> customerEntry : customerToOrdersMap.entrySet()) {
        Customer customer = customerEntry.getKey();
        Map<String, Object> customerData = new HashMap<>();
        customerData.put("customerName", customer.getName());
        customerData.put("customerPhone", customer.getPhone());

        Map<Address, List<Orders>> addressToOrdersMap = customerEntry.getValue().stream()
                .filter(order -> order.getAddress() != null)
                .collect(Collectors.groupingBy(Orders::getAddress));

        List<Map<String, Object>> addressDataList = new ArrayList<>();

        for (Map.Entry<Address, List<Orders>> addressEntry : addressToOrdersMap.entrySet()) {
            Address address = addressEntry.getKey();
            Map<String, Object> addressData = new HashMap<>();
            addressData.put("addressId", address.getAddressId());
            addressData.put("addressName", address.getAddressName());

            Map<Packing, List<Warehouse>> packingToWarehousesMap = addressEntry.getValue().stream()
                    .flatMap(order -> order.getWarehouses().stream())
                    .filter(warehouse -> warehouse.getPacking() != null)
                    .collect(Collectors.groupingBy(Warehouse::getPacking));

            List<Map<String, Object>> packingsData = new ArrayList<>();

            for (Map.Entry<Packing, List<Warehouse>> packingEntry : packingToWarehousesMap.entrySet()) {
                Packing packing = packingEntry.getKey();

                Set<String> trackingCodes = packingEntry.getValue().stream()
                        .filter(warehouse -> warehouse.getOrderLinks().stream()
                                .anyMatch(orderLink -> orderLink.getStatus() == OrderLinkStatus.CHO_GIAO))
                        .map(Warehouse::getTrackingCode)
                        .collect(Collectors.toSet());

                if (trackingCodes.isEmpty()) continue;

                Map<String, Object> packingData = new HashMap<>();
                packingData.put("packingCode", packing.getPackingCode());
                packingData.put("trackingCodes", trackingCodes);
                packingsData.add(packingData);
            }

            if (packingsData.isEmpty()) continue;

            addressData.put("packings", packingsData);
            addressDataList.add(addressData);
        }

        if (addressDataList.isEmpty()) continue;

        customerData.put("addresses", addressDataList);
        result.add(customerData);
    }

    return result;
}
    public Optional<Domestic> getDomesticById(Long id) {
        return domesticRepository.findById(id);
    }
}
