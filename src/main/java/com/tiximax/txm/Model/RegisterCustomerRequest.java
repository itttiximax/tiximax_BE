package com.tiximax.txm.Model;

import com.tiximax.txm.Entity.AccountRoute;
import com.tiximax.txm.Enums.AccountRoles;
import com.tiximax.txm.Enums.CustomerType;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Getter
@Setter

public class RegisterCustomerRequest {

    private String username;

    private String password;

    private String email;

    private String phone;

    private String name;

    private AccountRoles role;

    private CustomerType type;

    private String address;

    private String taxCode;

    private String source;

    private List<Long> routeIds;

}
