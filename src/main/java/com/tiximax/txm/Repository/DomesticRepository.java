package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.Domestic;
import com.tiximax.txm.Enums.DomesticStatus;
import com.tiximax.txm.Enums.PackingStatus;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository

public interface DomesticRepository extends JpaRepository<Domestic, Long> {

    @Query("SELECT d FROM Domestic d WHERE d.status = :status AND d.timestamp >= :start AND d.timestamp < :end")
    List<Domestic> findDeliveredToday(
    @Param("status") DomesticStatus status,
    @Param("start") LocalDateTime startOfDay,
    @Param("end") LocalDateTime endOfDay
);


}
