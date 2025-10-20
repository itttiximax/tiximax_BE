package com.tiximax.txm.API;

import com.tiximax.txm.Entity.Account;
import com.tiximax.txm.Entity.Customer;
import com.tiximax.txm.Entity.Staff;
import com.tiximax.txm.Enums.AccountRoles;
import com.tiximax.txm.Enums.AccountStatus;
import com.tiximax.txm.Model.*;
import com.tiximax.txm.Service.AuthenticationService;
import com.tiximax.txm.Service.EmailService;
import com.tiximax.txm.Service.OtpService;
import com.tiximax.txm.Service.TokenService;
import com.tiximax.txm.Utils.AccountUtils;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
//import net.minidev.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.json.JSONObject;



@RestController
@CrossOrigin
@RequestMapping("/accounts")
@SecurityRequirement(name = "bearerAuth")

public class AuthenticationController {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private AccountUtils accountUtils;

    @Autowired
    private OtpService otpService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private TokenService tokenService;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseAnonKey;

    private final WebClient webClient = WebClient.create();

    @PostMapping("/login")
    public ResponseEntity login(@RequestBody LoginRequest loginRequest) {
        try {
        Object account = authenticationService.login(loginRequest);
        return ResponseEntity.ok(account);
         } catch (AuthenticationServiceException e) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", e.getMessage()));
    }
}
  
    @PostMapping("/verify")
    public ResponseEntity<?> verifyToken(@RequestHeader("Authorization") String authorizationHeader) {
        try {

       
        String token = authorizationHeader.replace("Bearer ", "").trim();

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(supabaseUrl + "/auth/v1/user"))
                .header("Authorization", "Bearer " + token)
                .header("apikey", supabaseAnonKey)
                .GET()
                .build();
     
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JSONObject json = new JSONObject(response.body());
            String email = json.optString("email", "");
            JSONObject metadata = json.optJSONObject("user_metadata");
            String name = metadata != null ? metadata.optString("full_name", "") : "";

            Account account = authenticationService.verifyAndSaveUser(email, name);

            String appJwt = tokenService.generateToken(account);

            return ResponseEntity.ok(Map.of(
                    "message", "Login success",
                    "jwt", appJwt,
                    "user", Map.of(
                        "id", account.getAccountId(),
                        "email", account.getEmail(),
                        "name", account.getName(),
                        "role", account.getRole()
                    )
            ));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid Supabase token"));
        }

    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    }
}

    

    @PostMapping("/register/staff")
    public ResponseEntity<Staff> registerStaff(@RequestBody RegisterStaffRequest registerRequest) {
        Staff staff = authenticationService.registerStaff(registerRequest);
        return ResponseEntity.ok(staff);
    }

    @PostMapping("/register/customer")
    public ResponseEntity<?> registerCustomer(@RequestBody RegisterCustomerRequest registerRequest) {
    try {
        Customer customer = authenticationService.registerCustomer(registerRequest);
        return ResponseEntity.ok(customer);
    } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
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
        emailDetail.setRecipient("thinhnguyen.231003@gmail.com");
        emailDetail.setSubject("test123");
        emailDetail.setMsgBody("abc");
        emailService.sendMailTemplate(emailDetail);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        authenticationService.logout();
        return ResponseEntity.ok("Đăng xuất thành công!");
    }

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

    @GetMapping("/staff/{page}/{size}")
    public ResponseEntity<Page<Staff>> getAllStaff(@PathVariable int page, @PathVariable int size) {
        Sort sort = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Staff> staffPage = authenticationService.getAllStaff(pageable);
        return ResponseEntity.ok(staffPage);
    }

    @GetMapping("/customers/{page}/{size}")
    public ResponseEntity<Page<Customer>> getAllCustomers(@PathVariable int page, @PathVariable int size) {
        Sort sort = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Customer> customersPage = authenticationService.getAllCustomers(pageable);
        return ResponseEntity.ok(customersPage);
    }

    @GetMapping("/my-customers/{page}/{size}")
    public ResponseEntity<Page<Customer>> getCustomersByStaff(@PathVariable int page, @PathVariable int size) {
        Sort sort = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Customer> customersPage = authenticationService.getCustomersByStaff(pageable);
        return ResponseEntity.ok(customersPage);
    }

    @GetMapping("/sale-lead-staff/{page}/{size}")
    public ResponseEntity<Page<Staff>> getSaleLeadStaff(@PathVariable int page, @PathVariable int size) {
        Sort sort = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Staff> staffPage = authenticationService.getSaleAndLeadSaleStaff(pageable);
        return ResponseEntity.ok(staffPage);
    }

    @GetMapping("/sales-in-route/{page}/{size}")
    public ResponseEntity<Page<Staff>> getSalesInSameRoute(@PathVariable int page, @PathVariable int size) {
        Sort sort = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Staff> salesPage = authenticationService.getSalesInSameRoute(pageable);
        return ResponseEntity.ok(salesPage);
    }

    @GetMapping("/sales-in-route/stats")
    public ResponseEntity<List<SaleStats>> getSalesStatsInSameRoute(
            @RequestParam(required = false) String timeFrame,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer week) {
        List<SaleStats> stats = authenticationService.getSalesStatsInSameRoute(timeFrame, year, month, week);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/login-google")
    public ResponseEntity<String> loginWithGoogle() {
        String url = supabaseUrl + "/auth/v1/authorize?provider=google";
        return ResponseEntity.ok(url);
    }

    // ✅ Step 2: Nhận code từ Google và đổi token từ Supabase
    @GetMapping("/callback")
    public ResponseEntity<String> callback(@RequestParam("code") String code) {

        String tokenUrl = supabaseUrl + "/auth/v1/token?grant_type=authorization_code";

        String body = "{ \"code\": \"" + code + "\", " +
                "\"redirect_to\": \"http://localhost:8080/accounts/callback\" }";

        String response = webClient.post()
                .uri(tokenUrl)
                .header("apikey", supabaseAnonKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/userinfo")
    public ResponseEntity<String> getUserInfo(@RequestHeader("Authorization") String bearerToken) {

        String response = webClient.get()
                .uri(supabaseUrl + "/auth/v1/user")
                .header("apikey", supabaseAnonKey)
                .header("Authorization", bearerToken)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return ResponseEntity.ok(response);
    }
    @PostMapping("/verify-account")
    public ResponseEntity<?> verifyAccount(@RequestBody VerifyAccountRequest request) throws Exception {
       try {
            boolean isValid = otpService.validateOtp(request.getEmail(), request.getOtpCode());

            if (isValid) {
                return ResponseEntity.ok("Tài khoản của bạn đã được xác minh thành công!");
            } else {
                return ResponseEntity.badRequest().body("Mã OTP không hợp lệ hoặc đã hết hạn!");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
