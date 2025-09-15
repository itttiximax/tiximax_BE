package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.Warehouse;
import com.tiximax.txm.Entity.Websites;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository

public interface WebsiteRepository extends JpaRepository<Websites, Long> {

    @Query("SELECT w FROM Websites w WHERE LOWER(w.websiteName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Websites> findByWebsiteNameContaining(@Param("keyword") String keyword);

}
