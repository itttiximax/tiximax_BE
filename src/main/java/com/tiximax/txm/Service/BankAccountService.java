package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.BankAccount;
import com.tiximax.txm.Repository.BankAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BankAccountService {
    @Autowired
    private BankAccountRepository bankAccountRepository;

    public BankAccount save(BankAccount bankAccount) {
        return bankAccountRepository.save(bankAccount);
    }

    public Optional<BankAccount> findById(Long id) {
        return bankAccountRepository.findById(id);
    }

    public List<BankAccount> findAll() {
        return bankAccountRepository.findAll();
    }

    public void deleteById(Long id) {
        bankAccountRepository.deleteById(id);
    }

    public List<BankAccount> findByProxyAndRevenue(Boolean isProxy, Boolean isRevenue) {
        return bankAccountRepository.findByIsProxyAndIsRevenue(isProxy, isRevenue);
    }

    public BankAccount getAccountById(long id){
        return bankAccountRepository.findBankAccountById(id);
    }
}