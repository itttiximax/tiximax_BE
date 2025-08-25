//package com.tiximax.txm.Service;
//
//import com.tiximax.txm.Entity.Orders;
//import com.tiximax.txm.Entity.Packing;
//import com.tiximax.txm.Repository.OrdersRepository;
//import com.tiximax.txm.Repository.PackingRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//
//@Service
//
//public class PackingService {
//
//    @Autowired
//    private PackingRepository packingRepository;
//
//    @Autowired
//    private OrdersRepository ordersRepository;
//
//    public Packing createPackingFromOrders(List<Long> orderIds) {
//        List<Orders> orders = ordersRepository.findAllById(orderIds);
//
//        if (orders.isEmpty()) {
//            throw new RuntimeException("No orders found");
//        }
//
//        Packing packing = new Packing();
//        packing.setOrders(orders);
//        packing.setDescription("Packing list for orders: " + orderIds);
//
//        int totalQuantity = orders.stream().mapToInt(Orders::getTotalQuantity).sum();
//        double totalWeight = orders.stream().mapToDouble(Orders::getTotalWeight).sum();
//
//        packing.setTotalQuantity(totalQuantity);
//        packing.setTotalWeight(totalWeight);
//        packing.setTotalPackages(orders.size()); // ví dụ: mỗi order = 1 kiện
//
//        return packingRepository.save(packing);
//    }
//
//}
