package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.Orders;
import com.tiximax.txm.Entity.Packing;
import com.tiximax.txm.Enums.OrderStatus;
import com.tiximax.txm.Enums.PackingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PackingRepository extends JpaRepository<Packing, Long> {

    boolean existsByPackingCode(String packingCode);

    List<Packing> findByPackingCodeStartingWith(String baseCode);

    Page<Packing> findByFlightCodeIsNull(Pageable pageable);

    Page<Packing> findByFlightCodeIsNullAndWarehouses_Location_LocationId(Long warehouseLocationId, Pageable pageable);

    @Query("SELECT p FROM Packing p JOIN p.warehouses w WHERE p.status = :status AND w.location.locationId = :warehouseLocationId")
    Page<Packing> findByStatusAndWarehouses_Location_LocationId(
            @Param("status") PackingStatus status,
            @Param("warehouseLocationId") Long warehouseLocationId,
            Pageable pageable);
}