package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.Voucher;
import com.tiximax.txm.Enums.AssignType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    List<Voucher> findByAssignType(AssignType assignType);
    Voucher findByCode(String code);
}