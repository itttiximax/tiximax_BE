package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.Destination;
import com.tiximax.txm.Entity.MergedPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository

public interface MergedPaymentRepository extends JpaRepository<MergedPayment, Long> {

    boolean existsByPaymentCode(String paymentCode);
}