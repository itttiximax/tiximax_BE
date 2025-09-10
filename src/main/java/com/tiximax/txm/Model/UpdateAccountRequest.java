package com.tiximax.txm.Model;

import com.tiximax.txm.Enums.AccountRoles;
import com.tiximax.txm.Enums.AccountStatus;
import com.tiximax.txm.Enums.StaffDepartment;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data   
@Getter
@Setter

public class UpdateAccountRequest {
    private String name;
    private String email;
    private String phone;
    private AccountRoles role;
    private AccountStatus status;
    private String address;
    private String source;
    private StaffDepartment department;
    private String location;
}