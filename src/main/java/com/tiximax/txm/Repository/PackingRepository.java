package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.Packing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PackingRepository extends JpaRepository<Packing, Long> {

    boolean existsByPackingCode(String packingCode);

    List<Packing> findByPackingCodeStartingWith(String baseCode);

    Page<Packing> findByFlightCodeIsNull(Pageable pageable);

}