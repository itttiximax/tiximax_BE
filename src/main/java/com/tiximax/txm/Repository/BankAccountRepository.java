package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
    List<BankAccount> findByIsProxyAndIsRevenue(Boolean isProxy, Boolean isRevenue);
}