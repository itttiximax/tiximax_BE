package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.Warehouse;
import com.tiximax.txm.Enums.WarehouseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    boolean existsByTrackingCode(String trackingCode);

    @Query("SELECT w FROM Warehouse w WHERE w.status = :status")
    Page<Warehouse> findByStatus(@Param("status") WarehouseStatus status, Pageable pageable);

    List<Warehouse> findAllByStatus(WarehouseStatus warehouseStatus);
}