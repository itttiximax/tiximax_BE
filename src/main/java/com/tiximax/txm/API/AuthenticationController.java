package com.tiximax.txm.API;

import com.tiximax.txm.Entity.Account;
import com.tiximax.txm.Entity.Customer;
import com.tiximax.txm.Entity.Staff;
import com.tiximax.txm.Enums.AccountRoles;
import com.tiximax.txm.Enums.OrderType;
import com.tiximax.txm.Model.EmailDetail;
import com.tiximax.txm.Model.LoginRequest;
import com.tiximax.txm.Model.RegisterCustomerRequest;
import com.tiximax.txm.Model.RegisterStaffRequest;
import com.tiximax.txm.Service.AuthenticationService;
import com.tiximax.txm.Service.EmailService;
import com.tiximax.txm.Service.TokenService;
import com.tiximax.txm.Utils.AccountUtils;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Arrays;
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

    @Autowired
    private TokenService tokenService;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseAnonKey;

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

    @PostMapping("/register/customer/by-staff")
    public ResponseEntity<Customer> registerCustomerByStaff(@RequestBody RegisterCustomerRequest registerRequest) {
        Customer customer = authenticationService.registerCustomerByStaff(registerRequest);
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

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        authenticationService.logout();
        return ResponseEntity.ok("Đăng xuất thành công!");
    }

//    @GetMapping("/enum-customer-type")
//    public ResponseEntity<List<String>> getCustomerType() {
//        List<String> customerType = Arrays.stream(CustomerType.values())
//                .map(Enum::name)
//                .toList();
//        return ResponseEntity.ok(customerType);
//    }

//    @GetMapping("/login-google")
//    public ResponseEntity<?> initiateGoogleLogin() {
//        String redirectUrl = supabaseUrl + "/auth/v1/authorize?provider=google&redirect_to=http://localhost:8080/accounts/callback";
//        return ResponseEntity.ok().body("{\"redirect\": \"" + redirectUrl + "\"}");
//    }

//    @GetMapping("/callback")
//    public Mono<ResponseEntity<?>> handleCallback(@AuthenticationPrincipal OAuth2User principal, HttpServletRequest request) {
//        System.out.println("Callback called with principal: " + principal); // Log debug
//        if (principal == null) {
//            String accessToken = request.getParameter("access_token"); // Thử lấy từ query nếu có
//            if (accessToken != null) {
//                // Xử lý token từ Supabase nếu cần, nhưng ưu tiên OAuth2
//            }
//            return Mono.just(ResponseEntity.status(401).body("{\"error\": \"Principal null - Empty token\"}"));
//        }
//        String email = principal.getAttribute("email");
//        String name = principal.getAttribute("name");
//        if (email == null || name == null) {
//            return Mono.just(ResponseEntity.status(400).body("{\"error\": \"Missing info\"}"));
//        }
//        Account account = authenticationService.findOrCreateGoogleAccount(email, name);
//        if (account == null) {
//            return Mono.just(ResponseEntity.status(500).body("{\"error\": \"Save DB failed\"}"));
//        }
//        String jwt = tokenService.generateToken(account);
//        return Mono.just(ResponseEntity.ok("{\"jwt\": \"" + jwt + "\", \"user\": \"" + name + " (" + email + ")\"}"));
//    }

    @GetMapping("/search")
    public ResponseEntity<List<Customer>> searchCustomers(@RequestParam(required = false) String keyword) {
        List<Customer> customers = authenticationService.searchCustomersByPhoneOrName(keyword);
        return ResponseEntity.ok(customers);
    }

    @GetMapping("/enum-account-role")
    public ResponseEntity<List<String>> getAccountrole() {
        List<String> accountRole = Arrays.stream(AccountRoles.values())
                .map(Enum::name)
                .toList();
        return ResponseEntity.ok(accountRole);
    }

}
