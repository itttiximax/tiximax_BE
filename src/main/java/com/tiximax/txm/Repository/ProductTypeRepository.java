package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository

public interface ProductTypeRepository extends JpaRepository<ProductType, Long> {

    Optional<ProductType> findByProductTypeName(String productTypeName);

}
