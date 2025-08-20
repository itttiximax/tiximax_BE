package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByCustomerCode(String customerCode);
}