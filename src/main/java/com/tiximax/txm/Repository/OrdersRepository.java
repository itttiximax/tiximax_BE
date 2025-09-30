package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.Orders;
import com.tiximax.txm.Enums.OrderLinkStatus;
import com.tiximax.txm.Enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository

public interface OrdersRepository extends JpaRepository<Orders, Long> {

    Orders findByOrderCode(String orderCode);

    boolean existsByOrderCode(String orderCode);

    List<Orders> findAllByOrderCodeIn(List<String> orderCodes);

    @Query("SELECT o FROM Orders o WHERE :status IS NULL OR o.status = :status")
    Page<Orders> findByStatus(@Param("status") OrderStatus status, Pageable pageable);

    @Query("SELECT o FROM Orders o WHERE o.staff.accountId = :staffId AND (:status IS NULL OR o.status = :status)")
    Page<Orders> findByStaffAccountIdAndStatus(@Param("staffId") Long staffId, @Param("status") OrderStatus status, Pageable pageable);

    @Query("SELECT o FROM Orders o WHERE o.route.routeId IN :routeIds AND (:status IS NULL OR o.status = :status)")
    Page<Orders> findByRouteRouteIdInAndStatus(@Param("routeIds") Set<Long> routeIds, @Param("status") OrderStatus status, Pageable pageable);

    @Query("SELECT DISTINCT o FROM Orders o " +
            "LEFT JOIN FETCH o.payments p " +
            "LEFT JOIN FETCH p.relatedOrders ro " +
            "WHERE o.staff.accountId = :staffId AND o.status = :status")
    Page<Orders> findByStaffAccountIdAndStatusForPayment(@Param("staffId") Long staffId, @Param("status") OrderStatus status, Pageable pageable);

//    @Query("SELECT o FROM Orders o WHERE o.staff.accountId = :staffId AND o.status IN (:statuses) " +
//            "AND (o.status != :dangXuLyStatus OR EXISTS (SELECT ol FROM o.orderLinks ol WHERE ol.status = :daNhapKhoVnStatus))")
//    Page<Orders> findByStaffAccountIdAndStatusForPayment(
//            @Param("staffId") Long staffId,
//            @Param("statuses") List<OrderStatus> statuses,
//            @Param("dangXuLyStatus") OrderStatus dangXuLyStatus,
//            @Param("daNhapKhoVnStatus") OrderLinkStatus daNhapKhoVnStatus,
//            Pageable pageable);

    @Query("SELECT o FROM Orders o LEFT JOIN FETCH o.orderLinks WHERE o.route.routeId IN :routeIds AND o.status = :status")
    Page<Orders> findByRouteRouteIdInAndStatusWithLinks(@Param("routeIds") Set<Long> routeIds, @Param("status") OrderStatus status, Pageable pageable);

    long countByStaffAccountIdAndStatus(Long staffId, OrderStatus status);

    @Query("SELECT o FROM Orders o WHERE o.customer.customerCode = :customerCode AND o.status = :status")
    List<Orders> findByCustomerCodeAndStatus(@Param("customerCode") String customerCode, @Param("status") OrderStatus status);

    @Query("SELECT o FROM Orders o LEFT JOIN FETCH o.warehouses w LEFT JOIN FETCH w.orderLinks WHERE o.status = :status")
    Page<Orders> findByStatusWithWarehousesAndLinks(@Param("status") OrderStatus status, Pageable pageable);

    @Query("SELECT o FROM Orders o JOIN o.warehouses w WHERE o.status = :status AND w.location.locationId = :warehouseLocationId")
    Page<Orders> findByStatusWithWarehousesAndLinksAndWarehouseLocation(
            @Param("status") List<OrderStatus> status,
            @Param("warehouseLocationId") Long warehouseLocationId,
            Pageable pageable);
}