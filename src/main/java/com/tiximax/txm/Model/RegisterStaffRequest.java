package com.tiximax.txm.Model;

import com.tiximax.txm.Entity.AccountRoute;
import com.tiximax.txm.Enums.AccountRoles;
import com.tiximax.txm.Enums.StaffDepartment;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Getter
@Setter

public class RegisterStaffRequest {

    private String username;

    private String password;

    private String email;

    private String phone;

    private String name;

    private AccountRoles role;

    private StaffDepartment department;

    private String location;

    private List<Long> routeIds;

}
