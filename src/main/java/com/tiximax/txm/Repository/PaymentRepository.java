package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.Payment;
import com.tiximax.txm.Enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByPaymentCode(String paymentCode);

    List<Payment> findByOrdersOrderCode(String orderCode);

    Optional<Payment> findByPaymentCode(String paymentCode);

    void deleteByPaymentCode(String paymentCode);

    Optional<Payment> findFirstByOrdersOrderIdAndStatus(Long orderId, PaymentStatus paymentStatus);

}