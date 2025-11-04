package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.OrderLinks;
import com.tiximax.txm.Entity.Purchases;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Repository

public interface PurchasesRepository extends JpaRepository<Purchases, Long> {

    boolean existsByPurchaseCode(String purchaseCode);

    @Query("SELECT COALESCE(SUM(p.finalPriceOrder), 0) FROM Purchases p WHERE p.orders.orderId = :orderId")
    BigDecimal getTotalFinalPriceByOrderId(@Param("orderId") Long orderId);

    @Query("""
    SELECT DISTINCT p FROM Purchases p
    JOIN FETCH p.orderLinks ol
    JOIN p.orders o
    WHERE o.route.routeId IN :routeIds
      AND EXISTS (
        SELECT 1 FROM OrderLinks link
        WHERE link.purchase = p
          AND (link.shipmentCode IS NULL OR link.shipmentCode = '')
      )
    """)
    Page<Purchases> findPurchasesWithPendingShipmentByRoutes(
            @Param("routeIds") Set<Long> routeIds,
            Pageable pageable
    );
}
