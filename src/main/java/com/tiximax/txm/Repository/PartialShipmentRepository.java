package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.PartialShipment;
import com.tiximax.txm.Entity.Payment;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PartialShipmentRepository  extends JpaRepository<PartialShipment, Long> {
     List<PartialShipment> findByPayment(Payment payment);
}
