package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.Orders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository

public interface OrdersRepository extends JpaRepository<Orders, Long> {

    Orders findByOrderCode(String orderCode);

    boolean existsByOrderCode(String orderCode);

    List<Orders> findAllByOrderCodeIn(List<String> orderCodes);

    Page<Orders> findByStaffAccountId(Long staffId, Pageable pageable);

//    Page<Orders> findByRouteIdIn(Set<Long> routeIds, Pageable pageable);
}