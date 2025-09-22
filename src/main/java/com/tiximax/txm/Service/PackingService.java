package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.Destination;
import com.tiximax.txm.Entity.Orders;
import com.tiximax.txm.Entity.Packing;
import com.tiximax.txm.Entity.Staff;
import com.tiximax.txm.Entity.Warehouse;
import com.tiximax.txm.Enums.OrderStatus;
import com.tiximax.txm.Enums.ProcessLogAction;
import com.tiximax.txm.Model.PackingEligibleOrder;
import com.tiximax.txm.Model.PackingRequest;
import com.tiximax.txm.Repository.DestinationRepository;
import com.tiximax.txm.Repository.OrdersRepository;
import com.tiximax.txm.Repository.PackingRepository;
import com.tiximax.txm.Repository.WarehouseRepository;
import com.tiximax.txm.Utils.AccountUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private AccountUtils accountUtils;

    public Page<PackingEligibleOrder> getEligibleOrdersForPacking(Pageable pageable) {
        Page<Orders> ordersPage = ordersRepository.findByStatusWithWarehousesAndLinks(OrderStatus.CHO_DONG_GOI, pageable);

        return ordersPage.map(order -> {
            Map<String, Integer> trackingToCount = new HashMap<>();
            Set<Warehouse> warehouses = order.getWarehouses();
            for (Warehouse warehouse : warehouses) {
                String trackingCode = warehouse.getTrackingCode();
                int productCount = warehouse.getOrderLinks().size();
                trackingToCount.put(trackingCode, productCount);
            }

            PackingEligibleOrder eligibleOrder = new PackingEligibleOrder();
            eligibleOrder.setOrderCode(order.getOrderCode());
            eligibleOrder.setTrackingCodeToProductCount(trackingToCount);
            return eligibleOrder;
        });
    }

//    @Transactional
//    public Packing createPacking(PackingRequest request) {
//
//        Object currentAccount = accountUtils.getAccountCurrent();
//        if (!(currentAccount instanceof Staff)) {
//            throw new IllegalStateException("Chỉ nhân viên được phép tạo đóng gói!");
//        }
//        Staff staff = (Staff) currentAccount;
//
//        List<Orders> orders = ordersRepository.findAllByOrderCodeIn(request.getOrderCodes());
//        if (orders.isEmpty()) {
//            throw new IllegalArgumentException("Không tìm thấy đơn hàng nào trong danh sách cung cấp!");
//        }
//
//        for (Orders order : orders) {
//            if (!order.getStatus().equals(OrderStatus.CHO_DONG_GOI)) {
//                throw new IllegalArgumentException("Đơn hàng " + order.getOrderCode() + " chưa đủ điều kiện để đóng gói!");
//            }
//        }
//
//        Destination destination = destinationRepository.findById(request.getDestinationId())
//                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy điểm đến!"));
//
//        for (Orders order : orders) {
//            if (!order.getDestination().getDestinationId().equals(destination.getDestinationId())) {
//                throw new IllegalArgumentException("Đơn hàng " + order.getOrderCode() + " có điểm đến không khớp!");
//            }
//        }
//
//        List<String> packingList = orders.stream()
//                .map(Orders::getOrderCode)
//                .collect(Collectors.toList());
//
//        if (packingList.isEmpty()) {
//            throw new IllegalArgumentException("Không có mã đơn hàng nào để tạo packing list!");
//        }
//
//        Packing packing = new Packing();
//        packing.setFlightCode(request.getFlightCode());
//        packing.setPackingCode(generatePackingCode());
//        packing.setDestination(destination);
//        packing.setPackingList(packingList);
//        packing.setPackedDate(LocalDateTime.now());
//        packing.setStaff(staff);
//        packing = packingRepository.save(packing);
//
//        for (Orders order : orders) {
//            order.setStatus(OrderStatus.CHO_NHAP_KHO_VN);
//            order.setPacking(packing);
//            ordersRepository.save(order);
//        }
//        ordersService.addProcessLog(null, packing.getPackingCode(), ProcessLogAction.DA_DONG_GOI);
//
//        return packing;
//    }
//
//    public Packing getPackingById(Long packingId) {
//        return packingRepository.findById(packingId)
//                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy packing với ID: " + packingId));
//    }
//
//    public List<Packing> getAllPackings() {
//        return packingRepository.findAll();
//    }
//
//    public Packing updatePackingList(Long packingId, List<String> packingList) {
//        Packing packing = getPackingById(packingId);
//        packing.setPackingList(packingList);
//        return packingRepository.save(packing);
//    }
//
//    public String generatePackingCode() {
//        String packingCode;
//        do {
//            packingCode = "PK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();;
//        } while (packingRepository.existsByPackingCode(packingCode));
//        return packingCode;
//    }
}