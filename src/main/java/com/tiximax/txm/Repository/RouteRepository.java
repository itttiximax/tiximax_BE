package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route, Long> {

}
