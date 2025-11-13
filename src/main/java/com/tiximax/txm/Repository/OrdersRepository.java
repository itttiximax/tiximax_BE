package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.Orders;
import com.tiximax.txm.Enums.OrderStatus;
import com.tiximax.txm.Enums.OrderType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Repository

public interface OrdersRepository extends JpaRepository<Orders, Long> {

    Orders findByOrderCode(String orderCode);

    boolean existsByOrderCode(String orderCode);

    List<Orders> findAllByOrderCodeIn(List<String> orderCodes);

    @Query("SELECT o FROM Orders o WHERE :status IS NULL OR o.status = :status")
    Page<Orders> findByStatus(@Param("status") OrderStatus status, Pageable pageable);
    
        @Query(value = "SELECT DISTINCT o FROM Orders o " +
                "LEFT JOIN FETCH o.customer c " +
                "WHERE o.status IN :statuses",
        countQuery = "SELECT COUNT(o) FROM Orders o WHERE o.status IN :statuses")
        Page<Orders> findByStatuses(@Param("statuses") Collection<OrderStatus> statuses, Pageable pageable);



    @Query("SELECT o FROM Orders o WHERE o.staff.accountId = :staffId AND (:status IS NULL OR o.status = :status)")
    Page<Orders> findByStaffAccountIdAndStatus(@Param("staffId") Long staffId, @Param("status") OrderStatus status, Pageable pageable);

    @Query("SELECT o FROM Orders o WHERE o.route.routeId IN :routeIds AND (:status IS NULL OR o.status = :status)")
    Page<Orders> findByRouteRouteIdInAndStatus(@Param("routeIds") Set<Long> routeIds, @Param("status") OrderStatus status, Pageable pageable);

    @Query("SELECT DISTINCT o FROM Orders o " +
            "LEFT JOIN FETCH o.payments p " +
            "LEFT JOIN FETCH p.relatedOrders ro " +
            "WHERE o.staff.accountId = :staffId AND o.status = :status")
    Page<Orders> findByStaffAccountIdAndStatusForPayment(@Param("staffId") Long staffId, @Param("status") OrderStatus status, Pageable pageable);

    @Query("SELECT o FROM Orders o LEFT JOIN FETCH o.orderLinks WHERE o.route.routeId IN :routeIds AND o.status = :status")
    Page<Orders> findByRouteRouteIdInAndStatusWithLinks(@Param("routeIds") Set<Long> routeIds, @Param("status") OrderStatus status, Pageable pageable);

    long countByStaffAccountIdAndStatus(Long staffId, OrderStatus status);

    @Query("SELECT o FROM Orders o WHERE o.customer.customerCode = :customerCode AND o.status = :status")
    List<Orders> findByCustomerCodeAndStatus(@Param("customerCode") String customerCode, @Param("status") OrderStatus status);

List<Orders> findByCustomerCustomerCodeAndStatusIn(String customerCode, List<OrderStatus> statuses);

    @Query("SELECT o FROM Orders o LEFT JOIN FETCH o.warehouses w LEFT JOIN FETCH w.orderLinks WHERE o.status = :status")
    Page<Orders> findByStatusWithWarehousesAndLinks(@Param("status") OrderStatus status, Pageable pageable);

    Page<Orders> findByStatusInAndWarehouses_Location_LocationId(List<OrderStatus> statuses, Long locationId, Pageable pageable);

     @Query("SELECT DISTINCT o FROM Orders o " +
           "LEFT JOIN FETCH o.orderLinks " + 
           "WHERE o.route.routeId IN :routeIds " +
           "AND o.status = :status " +
           "AND o.orderType = :orderType")
    Page<Orders> findByRouteRouteIdInAndStatusAndOrderTypeWithLinks(
        @Param("routeIds") Set<Long> routeIds,
        @Param("status") OrderStatus status, 
        @Param("orderType") OrderType orderType,
        Pageable pageable
    );

    List<Orders> findByStaff_AccountIdAndRoute_RouteIdInAndCreatedAtBetween(Long accountId, Set<Long> routeIds, LocalDateTime startDate, LocalDateTime endDate);

    List<Orders> findByStaff_AccountIdAndRoute_RouteIdIn(Long accountId, Set<Long> routeIds);

    Page<Orders> findByStatusIn(List<OrderStatus> statuses, Pageable pageable);

    @Query("SELECT o FROM Orders o WHERE o.status IN :statuses " +
            "AND o.leftoverMoney IS NOT NULL " +
            "AND o.leftoverMoney < :threshold")
    Page<Orders> findByStatusInAndLeftoverMoneyLessThan(
            @Param("statuses") List<OrderStatus> statuses,
            @Param("threshold") BigDecimal threshold,
            Pageable pageable
    );

    @Query("SELECT o FROM Orders o WHERE o.staff.accountId = :staffId " +
            "AND o.status IN :statuses " +
            "AND o.leftoverMoney IS NOT NULL " +
            "AND o.leftoverMoney < :threshold")
    Page<Orders> findByStaffAccountIdAndStatusInAndLeftoverMoneyLessThan(
            @Param("staffId") Long staffId,
            @Param("statuses") List<OrderStatus> statuses,
            @Param("threshold") BigDecimal threshold,
            Pageable pageable
    );

    @Query("""
    SELECT DISTINCT o FROM Orders o
    JOIN FETCH o.orderLinks ol
    WHERE o.route.routeId IN :routeIds
      AND o.status = 'DANG_XU_LY'
      AND o.orderType = :orderType
      AND EXISTS (
        SELECT 1 FROM OrderLinks link
        WHERE link.orders = o AND link.status = 'MUA_SAU'
      )
    """)
    Page<Orders> findProcessingOrdersWithBuyLaterLinks(
            @Param("routeIds") Set<Long> routeIds,
            @Param("orderType") OrderType orderType,
            Pageable pageable
    );

    List<Orders> findByStaff_AccountIdAndCreatedAtBetween(Long accountId, LocalDateTime startDate, LocalDateTime endDate);
        Page<Orders> findByStaffAccountId(Long accountId, Pageable pageable);
        Page<Orders> findByRouteRouteIdIn(Set<Long> routeIds, Pageable pageable);

         long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}