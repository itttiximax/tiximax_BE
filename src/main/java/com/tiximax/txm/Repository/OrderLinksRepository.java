package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.OrderLinks;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository

public interface OrderLinksRepository extends JpaRepository<OrderLinks, Long> {

    boolean existsByTrackingCode(String orderLinkCode);

    List<OrderLinks> findByTrackingCodeIn(List<String> trackingCodes);

    List<OrderLinks> findByPurchasePurchaseId(Long purchaseId);

    List<OrderLinks> findByOrdersOrderId(Long orderId);

    List<OrderLinks> findByShipmentCode(String shipmentCode);
}
