package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.Destination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository

public interface DestinationRepository extends JpaRepository<Destination, Long> {

    Destination findByDestinationName(String destinationName);

    Destination findByDestinationId(Long destinationId);

}