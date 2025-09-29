//package com.tiximax.txm.Repository;
//
//import com.tiximax.txm.Entity.Domestic;
//import com.tiximax.txm.Enums.PackingStatus;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;
//
//@Repository
//
//public interface DomesticRepository extends JpaRepository<Domestic, Long> {
//
//    Page<Domestic> findByPacking_StatusAndLocation_LocationId(PackingStatus status, Long locationId, Pageable pageable);
//
//}
