package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
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

@Query("""
   SELECT c FROM Customer c
   WHERE c.staffId = :staffId
   AND (
        :keyword IS NULL
        OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(c.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(c.customerCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
   )
""")
Page<Customer> searchByStaff(
        @Param("staffId") Long staffId,
        @Param("keyword") String keyword,
        Pageable pageable
);

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.staffId = :staffId AND c.createdAt BETWEEN :startDate AND :endDate")
    long countByStaffIdAndCreatedAtBetween(@Param("staffId") Long staffId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.staffId = :staffId")
    long countByStaffId(@Param("staffId") Long staffId);

    Optional<Customer> findByCustomerCode(String customerCode);
    
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT c FROM Customer c WHERE c.accountId = :accountId")
    Customer getCustomerById(@Param("accountId") Long accountId);

    @Query("SELECT c FROM Customer c WHERE c.accountId = :customerId")
    Optional<Customer> findByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT c.customerCode FROM Customer c ORDER BY c.customerCode DESC LIMIT 1")
    String findLatestCustomerCode();

    List<Customer> findByCreatedAtBetween(Pageable pageable, LocalDateTime start, LocalDateTime end);
}