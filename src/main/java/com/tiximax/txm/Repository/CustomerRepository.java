package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByCustomerCode(String customerCode);

    @Query("SELECT c FROM Customer c WHERE (:keyword IS NULL OR (c.phone LIKE %:keyword% OR c.name LIKE %:keyword%)) AND c.staffId = :staffId")
    List<Customer> findByPhoneOrNameContainingAndStaffId(@Param("keyword") String keyword, @Param("staffId") Long staffId);

}