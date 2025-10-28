package com.tiximax.txm.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tiximax.txm.Entity.Address;

public interface AddressRepository extends JpaRepository<Address, Long>  {
    
}
