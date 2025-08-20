package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    boolean existsByTrackingCode(String trackingCode);

    Optional<Warehouse> findByTrackingCode(String trackingCode);

    List<Warehouse> findByOrdersOrderCode(String orderCode);

    List<Warehouse> findByPurchasePurchaseId(Long purchaseId);

    boolean existsByPurchasePurchaseId(Long purchaseId);
}