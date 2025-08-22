package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.Purchases;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository

public interface PurchasesRepository extends JpaRepository<Purchases, Long> {

    boolean existsByPurchaseCode(String purchaseCode);
}
