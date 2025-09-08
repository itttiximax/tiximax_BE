package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.Orders;
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

    Page<Orders> findByStaffAccountId(Long staffId, Pageable pageable);

    @Query("SELECT o FROM Orders o WHERE :status IS NULL OR o.status = :status")
    Page<Orders> findByStatus(@Param("status") OrderStatus status, Pageable pageable);

    @Query("SELECT o FROM Orders o WHERE o.staff.accountId = :staffId AND (:status IS NULL OR o.status = :status)")
    Page<Orders> findByStaffAccountIdAndStatus(@Param("staffId") Long staffId, @Param("status") OrderStatus status, Pageable pageable);

    @Query("SELECT o FROM Orders o WHERE o.route.routeId IN :routeIds AND (:status IS NULL OR o.status = :status)")
    Page<Orders> findByRouteRouteIdInAndStatus(@Param("routeIds") Set<Long> routeIds, @Param("status") OrderStatus status, Pageable pageable);

    @Query("SELECT o FROM Orders o WHERE o.status IN :statuses")
    Page<Orders> findByStatusIn(@Param("statuses") List<OrderStatus> statuses, Pageable pageable);

    @Query("SELECT o FROM Orders o WHERE o.staff.accountId = :staffId AND o.status = :status")
    Page<Orders> findByStaffAccountIdAndStatusForPayment(@Param("staffId") Long staffId, @Param("status") OrderStatus status, Pageable pageable);

}