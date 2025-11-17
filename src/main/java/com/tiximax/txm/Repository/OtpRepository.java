package com.tiximax.txm.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tiximax.txm.Entity.Account;
import com.tiximax.txm.Entity.Otp;

    public interface OtpRepository extends JpaRepository<Otp,Long> {
        Optional<Otp> findTopByAccount_EmailOrderByExpirationDesc(String email);
        Optional<Otp> findByAccountAndCode(Account account, String code);

}
    