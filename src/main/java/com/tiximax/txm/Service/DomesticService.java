//package com.tiximax.txm.Service;
//
//import com.tiximax.txm.Entity.Domestic;
//import com.tiximax.txm.Entity.Orders;
//import com.tiximax.txm.Entity.Packing;
//import com.tiximax.txm.Entity.Staff;
//import com.tiximax.txm.Enums.DomesticStatus;
//import com.tiximax.txm.Enums.PackingStatus;
//import com.tiximax.txm.Repository.DomesticRepository;
//import com.tiximax.txm.Repository.OrdersRepository;
//import com.tiximax.txm.Repository.PackingRepository;
//import com.tiximax.txm.Utils.AccountUtils;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.stereotype.Service;
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Set;
//
//@Service
//
//public class DomesticService {
//
//    @Autowired
//    private DomesticRepository domesticRepository;
//
//    @Autowired
//    private PackingRepository packingRepository;
//
//    @Autowired
//    private OrdersRepository ordersRepository;
//
//    @Autowired
//    private OrdersService ordersService;
//
//    @Autowired
//    private AccountUtils accountUtils;
//
//    public Page<Domestic> getDomesticsByPackingStatus(Pageable pageable) {
//        Staff staff = (Staff) accountUtils.getAccountCurrent();
//        if (staff == null || staff.getWarehouseLocation() == null) {
//            throw new IllegalArgumentException("Nhân viên hiện tại chưa được gán địa điểm kho!");
//        }
//
//        return domesticRepository.findByPacking_StatusAndLocation_LocationId(
//                PackingStatus.DA_BAY, staff.getWarehouseLocation().getLocationId(), pageable);
//    }

//    public List<Domestic> createDomestic(List<Long> packingIds, String note) {
//        Staff staff = (Staff) accountUtils.getAccountCurrent();
//        if (staff == null || staff.getWarehouseLocation() == null) {
//            throw new IllegalArgumentException("Nhân viên hiện tại chưa được gán địa điểm kho!");
//        }
//
//        if (packingIds == null || packingIds.isEmpty()) {
//            throw new IllegalArgumentException("Danh sách không được để trống!");
//        }
//
//        List<Packing> packings = packingRepository.findAllById(packingIds);
//        if (packings.size() != packingIds.size()) {
//            throw new IllegalArgumentException("Một số Packing không tồn tại!");
//        }
//
//        for (Packing packing : packings) {
//            if (packing.getStatus() != PackingStatus.DA_BAY) {
//                throw new IllegalArgumentException("Packing " + packing.getPackingCode() + " chưa đủ điều kiện nhập kho!");
//            }
//        }
//
//        List<Domestic> domestics = new ArrayList<>();
//        for (Packing packing : packings) {
//            Domestic domestic = new Domestic();
//            domestic.setPacking(packing);
////            domestic.setPackingCode(packing.getPackingCode());
//            domestic.setStatus(DomesticStatus.NHAN_HANG);
//            domestic.setTimestamp(LocalDateTime.now());
//            domestic.setStaff(staff);
//            domestic.setLocation(staff.getWarehouseLocation());
//            domestic.setFromLocation(staff.getWarehouseLocation());
//            domestic.setNote(note);
//
////            Set<Orders> orders = packing.getOrders();
////            if (orders.isEmpty()) {
////                throw new IllegalArgumentException("Packing " + packing.getPackingCode() + " không có đơn hàng liên quan!");
////            }
//
//            // Gán Order đầu tiên trong danh sách
////            Orders order = orders.iterator().next();
////            domestic.setOrders(order);
//
//            // Lưu Domestic
//            domestics.add(domesticRepository.save(domestic));
//
//            // Cập nhật trạng thái Packing
//            packing.setStatus(PackingStatus.RECEIVED);
//            packingRepository.save(packing);
//
//            // Cập nhật trạng thái Orders
//            for (Orders ord : orders) {
//                ord.setStatus(OrderStatus.DANG_VAN_CHUYEN_VE_VN);
//                ordersRepository.save(ord);
//            }
//
//            // Ghi log
//            ordersService.addProcessLog(null, packing.getPackingCode(), ProcessLogAction.DA_NHAN_TAI_VN);
//        }
//
//        return domestics;
//    }

//}
