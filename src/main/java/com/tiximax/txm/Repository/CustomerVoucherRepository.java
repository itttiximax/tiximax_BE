package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.Customer;
import com.tiximax.txm.Entity.CustomerVoucher;
import com.tiximax.txm.Entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerVoucherRepository extends JpaRepository<CustomerVoucher, Long> {
    boolean existsByCustomerAndVoucher(Customer customer, Voucher voucher);

    List<CustomerVoucher> findByCustomerAndIsUsedFalse(Customer customer);
}
