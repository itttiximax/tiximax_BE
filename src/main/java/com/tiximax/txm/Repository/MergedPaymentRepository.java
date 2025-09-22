//package com.tiximax.txm.Repository;
//
//import com.tiximax.txm.Entity.Destination;
//import com.tiximax.txm.Entity.MergedPayment;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import java.util.Optional;
//
//@Repository
//
//public interface MergedPaymentRepository extends JpaRepository<MergedPayment, Long> {
//
//    @Query("SELECT mp FROM MergedPayment mp LEFT JOIN FETCH mp.orders WHERE mp.paymentCode = :paymentCode")
//    Optional<MergedPayment> findByPaymentCodeWithOrders(@Param("paymentCode") String paymentCode);
//
//    boolean existsByPaymentCode(String paymentCode);
//}