package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByCustomerCode(String customerCode);

    @Query("SELECT c FROM Customer c WHERE (:keyword IS NULL OR (c.phone LIKE %:keyword% OR c.name LIKE %:keyword%)) AND c.staffId = :staffId")
    List<Customer> findByPhoneOrNameContainingAndStaffId(@Param("keyword") String keyword, @Param("staffId") Long staffId);

    Page<Customer> findByStaffId(Long staffId, Pageable pageable);

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.staffId = :staffId AND c.createdAt BETWEEN :startDate AND :endDate")
    long countByStaffIdAndCreatedAtBetween(@Param("staffId") Long staffId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.staffId = :staffId")
    long countByStaffId(@Param("staffId") Long staffId);

    Optional<Customer> findByCustomerCode(String customerCode);
}