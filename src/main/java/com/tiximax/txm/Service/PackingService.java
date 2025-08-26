package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.Destination;
import com.tiximax.txm.Entity.Orders;
import com.tiximax.txm.Entity.Packing;
import com.tiximax.txm.Entity.Staff;
import com.tiximax.txm.Entity.Warehouse;
import com.tiximax.txm.Enums.OrderStatus;
import com.tiximax.txm.Model.PackingRequest;
import com.tiximax.txm.Repository.DestinationRepository;
import com.tiximax.txm.Repository.OrdersRepository;
import com.tiximax.txm.Repository.PackingRepository;
import com.tiximax.txm.Repository.WarehouseRepository;
import com.tiximax.txm.Utils.AccountUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PackingService {

    @Autowired
    private PackingRepository packingRepository;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private DestinationRepository destinationRepository;

    @Autowired
    private AccountUtils accountUtils;

    public Packing createPacking(PackingRequest request) {

        Object currentAccount = accountUtils.getAccountCurrent();
        if (!(currentAccount instanceof Staff)) {
            throw new IllegalStateException("Chỉ nhân viên được phép tạo packing.");
        }
        Staff staff = (Staff) currentAccount;

        // Lấy danh sách Orders
        List<Orders> orders = ordersRepository.findAllById(request.getOrderIds());
        if (orders.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy đơn hàng nào trong danh sách cung cấp!");
        }

        for (Orders order : orders) {
            if (!order.getStatus().equals(OrderStatus.CHO_DONG_GOI)) {
                throw new IllegalArgumentException("Đơn hàng " + order.getOrderCode() + " chưa đủ điều kiện để đóng gói!");
            }
        }

        Destination destination = destinationRepository.findByDestinationId(request.getDestinationId());
        if (destination == null) {
            throw new IllegalArgumentException("Không tìm thấy điểm đến!");
        }

        for (Orders order : orders) {
            if (!order.getDestination().getDestinationId().equals(destination.getDestinationId())) {
                throw new IllegalArgumentException("Đơn hàng " + order.getOrderCode() + " có điểm đến không khớp!");
            }
        }

        List<String> packingList = new ArrayList<>();
        for (Orders order : orders) {
            List<Warehouse> warehouses = warehouseRepository.findByOrdersOrderCode(order.getOrderCode());
            packingList.addAll(warehouses.stream()
                    .map(Warehouse::getTrackingCode)
                    .collect(Collectors.toList()));
        }

        if (packingList.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy mã theo dõi nào cho các đơn hàng này!");
        }

        Packing packing = new Packing();
        packing.setFlightCode(request.getFlightCode());
        packing.setDestination(destination);
        packing.setPackingList(packingList);
        packing.setPackedDate(LocalDateTime.now());
        packing.setStaff(staff);
        packing.setOrders(Set.copyOf(orders));

        for (Orders order : orders) {
            order.setStatus(OrderStatus.CHO_NHAP_KHO_VN);
            order.setPacking(packing);
            ordersRepository.save(order);
        }

        return packingRepository.save(packing);
    }

    public Packing getPackingById(Long packingId) {
        return packingRepository.findById(packingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy packing với ID: " + packingId));
    }

    public List<Packing> getAllPackings() {
        return packingRepository.findAll();
    }

    public Packing updatePackingList(Long packingId, List<String> packingList) {
        Packing packing = getPackingById(packingId);
        packing.setPackingList(packingList);
        return packingRepository.save(packing);
    }
}