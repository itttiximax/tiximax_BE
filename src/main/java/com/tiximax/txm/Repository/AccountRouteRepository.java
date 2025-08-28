package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.AccountRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository

public interface AccountRouteRepository extends JpaRepository<AccountRoute, Long> {

}

