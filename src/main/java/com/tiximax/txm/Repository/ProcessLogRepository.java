package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.OrderProcessLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository

public interface ProcessLogRepository extends JpaRepository<OrderProcessLog, Long> {

}