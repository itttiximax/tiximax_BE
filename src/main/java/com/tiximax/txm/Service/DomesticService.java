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

import io.swagger.v3.oas.annotations.Operation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    public List<Map<String, Object>> getReadyForDeliveryOrdersByCustomerCode(String customerCode) {
    // 1. Lấy toàn bộ dữ liệu sẵn sàng giao
    List<Map<String, Object>> allReadyData = getReadyForDeliveryOrders(Pageable.unpaged());
    if (allReadyData.isEmpty()) {
        return Collections.emptyList();
    }

    // 2. Lọc theo customerCode
    return allReadyData.stream()
            .filter(customerData -> {
                String name = (String) customerData.get("customerName");
                List<Map<String, Object>> addresses = (List<Map<String, Object>>) customerData.get("addresses");
                if (addresses == null || addresses.isEmpty()) return false;

                // Lấy 1 packing đầu tiên để truy vết customer
                List<Map<String, Object>> packings = (List<Map<String, Object>>) addresses.get(0).get("packings");
                if (packings == null || packings.isEmpty()) return false;

                String firstPackingCode = (String) packings.get(0).get("packingCode");

                return packingRepository.findByPackingCode(firstPackingCode)
                        .flatMap(packing -> packing.getWarehouses().stream().findFirst())
                        .map(Warehouse::getOrders)
                        .map(Orders::getCustomer)
                        .map(Customer::getCustomerCode)
                        .map(code -> code.equalsIgnoreCase(customerCode))
                        .orElse(false);
            })
            .collect(Collectors.toList());
}
    
public List<DomesticResponse> transferByCustomerCode(String customerCode) {

    List<Map<String, Object>> readyData = getReadyForDeliveryOrders(Pageable.unpaged());
    if (readyData.isEmpty()) {
        return Collections.emptyList();
    }

    Optional<Map<String, Object>> customerDataOpt = readyData.stream()
            .filter(data -> {
                String name = (String) data.get("customerName");
                List<Map<String, Object>> addresses = (List<Map<String, Object>>) data.get("addresses");
                if (addresses == null || addresses.isEmpty()) return false;

                List<Map<String, Object>> packings = (List<Map<String, Object>>) addresses.get(0).get("packings");
                if (packings == null || packings.isEmpty()) return false;

                String firstPackingCode = (String) packings.get(0).get("packingCode");
                return packingRepository.findByPackingCode(firstPackingCode)
                        .map(p -> p.getWarehouses())
                        .flatMap(warehouses -> warehouses.stream().findFirst())
                        .map(Warehouse::getOrders)
                        .map(Orders::getCustomer)
                        .map(Customer::getCustomerCode)
                        .map(code -> code.equalsIgnoreCase(customerCode))
                        .orElse(false);
            })
            .findFirst();

    if (customerDataOpt.isEmpty()) {
        return Collections.emptyList();
    }

    Map<String, Object> customerData = customerDataOpt.get();
    String customerName = (String) customerData.get("customerName");
    List<Map<String, Object>> addresses = (List<Map<String, Object>>) customerData.get("addresses");

    Staff currentStaff = (Staff) accountUtils.getAccountCurrent();
    WarehouseLocation currentLocation = currentStaff != null ? currentStaff.getWarehouseLocation() : null;
    List<Domestic> createdDomestics = new ArrayList<>();

    for (Map<String, Object> addressData : addresses) {
        Long addressId = ((Number) addressData.get("addressId")).longValue();
        Address address = addressRepository.findById(addressId).orElse(null);
        if (address == null) continue;

        List<Map<String, Object>> packingsData = (List<Map<String, Object>>) addressData.get("packings");
        if (packingsData == null || packingsData.isEmpty()) continue;

        Set<Packing> packingSet = new HashSet<>();
        List<String> shippingList = new ArrayList<>();

        for (Map<String, Object> packingData : packingsData) {
            String packingCode = (String) packingData.get("packingCode");
            Packing packing = packingRepository.findByPackingCode(packingCode).orElse(null);
            if (packing == null) continue;

            packingSet.add(packing);
            Set<String> trackingCodes = (Set<String>) packingData.get("trackingCodes");
            if (trackingCodes != null) {
                shippingList.addAll(trackingCodes);
            }
        }

        if (packingSet.isEmpty()) continue;

        Domestic domestic = new Domestic();
        domestic.setPackings(packingSet);
        domestic.setShippingList(shippingList);
        domestic.setStatus(DomesticStatus.DA_GIAO);
        domestic.setTimestamp(LocalDateTime.now());
        domestic.setNote("Giao hàng tự động cho khách: " + customerName + " (Mã: " + customerCode + ")");
        domestic.setToAddress(address);
        domestic.setStaff(currentStaff);
        domestic.setFromLocation(currentLocation);
        domestic.setLocation(currentLocation);
        domestic.setToLocation(null);

        domestic = domesticRepository.save(domestic);
        createdDomestics.add(domestic);

        updateOrderLinksAndOrders(shippingList, domestic);
    }

    return createdDomestics.stream()
            .map(DomesticResponse::fromEntity)
            .collect(Collectors.toList());
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
        customerData.put("customerCode", customer.getCustomerCode());
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

    // trackingCodes
    Set<String> trackingCodes = packingEntry.getValue().stream()
            .filter(warehouse -> warehouse.getOrderLinks().stream()
                    .anyMatch(orderLink -> orderLink.getStatus() == OrderLinkStatus.CHO_GIAO))
            .map(Warehouse::getTrackingCode)
            .collect(Collectors.toSet());

    if (trackingCodes.isEmpty()) continue;

    // orderLinks
    List<Map<String, Object>> orderLinksList = packingEntry.getValue().stream()
            .flatMap(warehouse -> warehouse.getOrderLinks().stream())
            .filter(orderLink -> orderLink.getStatus() == OrderLinkStatus.CHO_GIAO)
            .map(orderLink -> {
                Map<String, Object> map = new HashMap<>();
                map.put("orderLinkId", orderLink.getLinkId());
                map.put("trackingCode", orderLink.getTrackingCode());
                map.put("price", orderLink.getPriceWeb());
                map.put("status", orderLink.getStatus());
                return map;
            })
            .collect(Collectors.toList());

            Map<String, Object> packingData = new HashMap<>();
            packingData.put("packingCode", packing.getPackingCode());
            packingData.put("trackingCodes", trackingCodes);
            packingData.put("orderLinks", orderLinksList);
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
   public List<DomesticResponse> getDomesticDeliveredOnDaily() {
    LocalDate today = LocalDate.now();
    LocalDateTime startOfDay = today.atStartOfDay();
    LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

    List<Domestic> domestics = domesticRepository.findDeliveredToday(
        DomesticStatus.DA_GIAO, startOfDay, endOfDay
    );

    return domestics.stream()
            .map(DomesticResponse::fromEntity)
            .collect(Collectors.toList());
}


private void updateOrderLinksAndOrders(List<String> shipmentCodes, Domestic domestic) {
    for (String shipmentCode : shipmentCodes) {
        List<OrderLinks> links = orderLinksRepository.findByShipmentCode(shipmentCode);
        for (OrderLinks link : links) {
            link.setStatus(OrderLinkStatus.DA_GIAO);
        }
        orderLinksRepository.saveAll(links);

        Set<Orders> orders = links.stream()
                .map(OrderLinks::getOrders)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (Orders order : orders) {
            boolean allDelivered = order.getOrderLinks().stream()
                    .allMatch(l -> l.getStatus() == OrderLinkStatus.DA_GIAO || l.getStatus() == OrderLinkStatus.DA_HUY);

            if (allDelivered && order.getStatus() != OrderStatus.DA_GIAO) {
                order.setStatus(OrderStatus.DA_GIAO);
                ordersRepository.save(order);
                ordersService.addProcessLog(order, domestic.getDomesticId().toString(), ProcessLogAction.DA_GIAO);
            }
        }

        // Xử lý PartialShipment tương tự
        Set<PartialShipment> partials = links.stream()
                .map(OrderLinks::getPartialShipment)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (PartialShipment partial : partials) {
            boolean allDone = partial.getReadyLinks().stream()
                    .allMatch(l -> l.getStatus() == OrderLinkStatus.DA_GIAO || l.getStatus() == OrderLinkStatus.DA_HUY);
            if (allDone && partial.getStatus() != OrderStatus.DA_GIAO) {
                partial.setStatus(OrderStatus.DA_GIAO);
                partialShipmentRepository.save(partial);
                ordersService.addProcessLog(partial.getOrders(), "PS#" + partial.getId(), ProcessLogAction.DA_GIAO);
            }
        }
    }
}
}
