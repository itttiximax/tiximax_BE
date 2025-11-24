package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.OrderLinks;
import com.tiximax.txm.Enums.OrderLinkStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

      @Query("""
          SELECT ol
          FROM OrderLinks ol
          WHERE ol.orders.customer.customerCode = :customerCode
            AND ol.shipmentCode IS NOT NULL
            AND ol.status = :status
            AND ol.partialShipment IS NULL
      """)
      List<OrderLinks> findLinksInWarehouseWithoutPartialShipment(
              @Param("customerCode") String customerCode,
              @Param("status") OrderLinkStatus status
      );

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
    
    @Query("SELECT COUNT(ol) FROM OrderLinks ol WHERE ol.orders.createdAt BETWEEN :start AND :end")
    long countByOrdersCreatedAtBetween(@Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);

    boolean existsByShipmentCodeAndLinkIdNot(String newShipmentCode, Long orderLinkId);
}
