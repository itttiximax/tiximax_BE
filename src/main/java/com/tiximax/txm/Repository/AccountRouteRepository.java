package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.AccountRoute;
import com.tiximax.txm.Entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository

public interface AccountRouteRepository extends JpaRepository<AccountRoute, Long> {

    @Query("SELECT ar.route FROM AccountRoute ar WHERE ar.account.accountId = :accountId")
    List<Route> findRoutesByStaffId(Long accountId);
}

