package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.Payment;
import com.tiximax.txm.Entity.Staff;
import com.tiximax.txm.Enums.OrderStatus;
import com.tiximax.txm.Enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByPaymentCode(String paymentCode);

    List<Payment> findByOrdersOrderCode(String orderCode);

    Optional<Payment> findByPaymentCode(String paymentCode);

    Optional<Payment> findFirstByOrdersOrderIdAndStatus(Long orderId, PaymentStatus paymentStatus);

    @Query("SELECT p FROM Payment p JOIN p.relatedOrders o WHERE o.orderId = :orderId AND p.isMergedPayment = true AND p.status = :status")
    Optional<Payment> findMergedPaymentByOrderIdAndStatus(@Param("orderId") Long orderId, @Param("status") PaymentStatus status);

    @Query("""
           SELECT p 
           FROM Payment p 
           JOIN p.orders o 
           WHERE p.staff = :staff 
             AND o.status = :orderStatus
             AND p.status = :paymentStatus
           ORDER BY p.actionAt DESC
           """)
    List<Payment> findAllByStaffAndOrderStatusAndPaymentStatusOrderByActionAtDesc(
            @Param("staff") Staff staff,
            @Param("orderStatus") OrderStatus orderStatus,
            @Param("paymentStatus") PaymentStatus paymentStatus
    );
}

