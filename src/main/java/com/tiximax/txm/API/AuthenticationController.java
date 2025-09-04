package com.tiximax.txm.API;

import com.tiximax.txm.Entity.Account;
import com.tiximax.txm.Entity.Customer;
import com.tiximax.txm.Entity.Staff;
import com.tiximax.txm.Enums.CustomerType;
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

    @GetMapping("/enum-customer-type")
    public ResponseEntity<List<String>> getCustomerType() {
        List<String> customerType = Arrays.stream(CustomerType.values())
                .map(Enum::name)
                .toList();
        return ResponseEntity.ok(customerType);
    }

    @GetMapping("/login-google")
    public ResponseEntity<?> initiateGoogleLogin() {
        String redirectUrl = supabaseUrl + "/auth/v1/authorize?provider=google&redirect_to=http://localhost:8080/accounts/callback";
        return ResponseEntity.ok().body("{\"redirect\": \"" + redirectUrl + "\"}");
    }

//    @GetMapping("/callback")
//    public Mono<ResponseEntity<?>> handleCallback(@AuthenticationPrincipal OAuth2User principal) {
//        if (principal == null) {
//            System.out.println("Principal is null, OAuth2 flow may not be triggered correctly.");
//            return Mono.just(ResponseEntity.status(401).body("{\"error\":\"OAuth2 authentication failed, principal is null\"}"));
//        }
//
//        String email = principal.getAttribute("email");
//        String name = principal.getAttribute("name");
//
//        Account account = authenticationService.findOrCreateGoogleAccount(email, name);
//
//        String jwt = tokenService.generateToken(account);
//
//        WebClient webClient = WebClient.builder()
//                .baseUrl(supabaseUrl)
//                .defaultHeader("apikey", supabaseAnonKey)
//                .build();
//
//        return webClient.get()
//                .uri("/auth/v1/user")
//                .header("Authorization", "Bearer " + principal.getAttribute("access_token"))
//                .retrieve()
//                .bodyToMono(String.class)
//                .map(userInfo -> ResponseEntity.ok("User logged in: " + name + " (" + email + "), JWT: " + jwt));
//    }

    @GetMapping("/callback")
    public Mono<ResponseEntity<?>> handleCallback(@AuthenticationPrincipal OAuth2User principal, HttpServletRequest request) {

        if (principal == null) {
            return Mono.just(ResponseEntity.status(401).body("{\"error\":\"OAuth2 authentication failed, principal is null\"}"));
        }

        String email = principal.getAttribute("email");
        String name = principal.getAttribute("name");

        if (email == null || name == null) {
            return Mono.just(ResponseEntity.status(400).body("{\"error\":\"Missing user info from OAuth2\"}"));
        }

        Account account = authenticationService.findOrCreateGoogleAccount(email, name);
        String jwt = tokenService.generateToken(account);

        // Gọi Supabase để verify (tùy chọn, có thể bỏ nếu không cần)
        WebClient webClient = WebClient.builder()
                .baseUrl(supabaseUrl)
                .defaultHeader("apikey", supabaseAnonKey)
                .build();

        String accessToken = principal.getAttribute("access_token");
        if (accessToken == null) {
            return Mono.just(ResponseEntity.ok("{\"jwt\": \"" + jwt + "\", \"user\": \"" + name + " (" + email + ")\"}"));
        }

        return webClient.get()
                .uri("/auth/v1/user")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(String.class)
                .map(userInfo -> {
                    return ResponseEntity.ok("{\"jwt\": \"" + jwt + "\", \"user\": \"" + name + " (" + email + ")\"}");
                });
    }

}
