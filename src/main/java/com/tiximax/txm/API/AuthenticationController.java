package com.tiximax.txm.API;

import com.tiximax.txm.Entity.Account;
import com.tiximax.txm.Entity.Customer;
import com.tiximax.txm.Entity.Staff;
import com.tiximax.txm.Model.EmailDetail;
import com.tiximax.txm.Model.LoginRequest;
import com.tiximax.txm.Model.RegisterCustomerRequest;
import com.tiximax.txm.Model.RegisterStaffRequest;
import com.tiximax.txm.Service.AuthenticationService;
import com.tiximax.txm.Service.EmailService;
import com.tiximax.txm.Utils.AccountUtils;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/accounts")
@SecurityRequirement(name = "bearerAuth")

public class AuthenticationController {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private AccountUtils accountUtils;

    @Autowired
    private EmailService emailService;

    @PostMapping("/login")
    public ResponseEntity login(@RequestBody LoginRequest loginRequest) {
        Object account = authenticationService.login(loginRequest);
        return ResponseEntity.ok(account);
    }

    @PostMapping("/register/staff")
    public ResponseEntity<Staff> registerStaff(@RequestBody RegisterStaffRequest registerRequest) {
        Staff staff = authenticationService.registerStaff(registerRequest);
        return ResponseEntity.ok(staff);
    }

    @PostMapping("/register/customer")
    public ResponseEntity<Customer> registerCustomer(@RequestBody RegisterCustomerRequest registerRequest) {
        Customer customer = authenticationService.registerCustomer(registerRequest);
        return ResponseEntity.ok(customer);
    }

    @GetMapping("/accountCurrent")
    public ResponseEntity<?> current() {
        Account account = accountUtils.getAccountCurrent();
        return ResponseEntity.ok(account);
    }

    @GetMapping("/send-mail")
    public void sendMail() {
        EmailDetail emailDetail = new EmailDetail();
        emailDetail.setRecipient("phatttse170312@fpt.edu.vn");
        emailDetail.setSubject("test123");
        emailDetail.setMsgBody("abc");
        emailService.sendMailTemplate(emailDetail);
    }

//    @PutMapping("/update-all-passwords")
//    public ResponseEntity<List<Account>> updateAllAccountsPasswordToOne() {
//        List<Account> updatedAccounts = authenticationService.updateAllAccountsPasswordToOne();
//        return ResponseEntity.ok(updatedAccounts);
//    }

}
