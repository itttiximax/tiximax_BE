package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<Address, Long> {
}
