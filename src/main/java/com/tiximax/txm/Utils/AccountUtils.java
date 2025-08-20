package com.tiximax.txm.Utils;

import com.tiximax.txm.Entity.Account;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component

public class AccountUtils {
    public Account getAccountCurrent(){
        return (Account) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
    public boolean isLoggedIn() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal instanceof UserDetails;
    }
}