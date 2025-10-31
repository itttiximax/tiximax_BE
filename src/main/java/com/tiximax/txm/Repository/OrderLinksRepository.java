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
    
    boolean existsByShipmentCode(String shipmentCode);

    List<OrderLinks> findByTrackingCodeIn(List<String> trackingCodes);

    List<OrderLinks> findByOrdersOrderId(Long orderId);

    @Query("SELECT ol FROM OrderLinks ol LEFT JOIN FETCH ol.orders WHERE ol.shipmentCode = :shipmentCode")
    List<OrderLinks> findByShipmentCode(@Param("shipmentCode") String shipmentCode);

    List<OrderLinks> findByShipmentCodeIn(List<String> shipmentCodes);

    // OrderLinksRepository.java
    @Query("""
            SELECT ol FROM OrderLinks ol 
            WHERE ol.status = 'DA_MUA' 
            AND (ol.shipmentCode IS NULL OR ol.shipmentCode = '')
            """)
    List<OrderLinks> findPendingShipmentLinks();

    @Query("""
    SELECT DISTINCT ol.shipmentCode
    FROM OrderLinks ol
    WHERE ol.status = 'DA_MUA'
      AND ol.shipmentCode IS NOT NULL
      AND ol.shipmentCode != ''
      AND (:keyword IS NULL OR ol.shipmentCode LIKE %:keyword%)
    """)
    List<String> suggestShipmentCodes(@Param("keyword") String keyword);
}
