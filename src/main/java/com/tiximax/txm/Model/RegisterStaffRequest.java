package com.tiximax.txm.Model;

import com.tiximax.txm.Enums.AccountRoles;
import com.tiximax.txm.Enums.StaffDepartment;
import com.tiximax.txm.Enums.StaffPosition;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

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

    private StaffPosition position;

    private String location;

}
