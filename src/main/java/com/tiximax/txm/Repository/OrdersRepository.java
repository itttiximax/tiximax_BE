package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.Orders;
import org.hibernate.query.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository

public interface OrdersRepository extends JpaRepository<Orders, Long> {

    Orders findByOrderCode(String orderCode);

    boolean existsByOrderCode(String orderCode);

}