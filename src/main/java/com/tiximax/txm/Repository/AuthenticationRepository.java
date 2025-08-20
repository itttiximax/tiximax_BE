package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.Account;
import com.tiximax.txm.Entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository

public interface AuthenticationRepository extends JpaRepository<Account, Long> {
    Account findByUsername(String username);

    Customer findByCustomerCode(String customerCode);
}
