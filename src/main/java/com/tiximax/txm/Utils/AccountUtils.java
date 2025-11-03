package com.tiximax.txm.Utils;

import com.tiximax.txm.Entity.Account;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component

public class AccountUtils {
       public Account getAccountCurrent() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof Account account) {
            return account; 
        }
        return null;
    }

    public boolean isLoggedIn() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal instanceof Account;
    }
}