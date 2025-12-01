package com.tiximax.txm.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tiximax.txm.Entity.WarehouseLocation;

@Repository
public interface WarehouseLocationRepository extends JpaRepository<WarehouseLocation, Long> {

}
