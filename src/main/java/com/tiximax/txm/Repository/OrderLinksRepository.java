package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.OrderLinks;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository

public interface OrderLinksRepository extends JpaRepository<OrderLinks, Long> {

    boolean existsByTrackingCode(String orderLinkCode);

    List<OrderLinks> findByTrackingCodeIn(List<String> trackingCodes);

    List<OrderLinks> findByOrdersOrderId(Long orderId);

//    List<OrderLinks> findByShipmentCodeWithOrders(String shipmentCode);

    @Query("SELECT ol FROM OrderLinks ol LEFT JOIN FETCH ol.orders WHERE ol.shipmentCode = :shipmentCode")
    List<OrderLinks> findByShipmentCode(@Param("shipmentCode") String shipmentCode);

}
