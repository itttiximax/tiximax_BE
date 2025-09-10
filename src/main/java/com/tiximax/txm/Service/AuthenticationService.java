package com.tiximax.txm.Service;

import com.tiximax.txm.Config.SecurityConfig;
import com.tiximax.txm.Entity.*;
import com.tiximax.txm.Enums.AccountRoles;
import com.tiximax.txm.Enums.AccountStatus;
import com.tiximax.txm.Model.*;
import com.tiximax.txm.Repository.*;
import com.tiximax.txm.Utils.AccountUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service

public class AuthenticationService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    @Autowired
    @Lazy
    private AuthenticationManager authenticationManager;

    @Autowired
    private SecurityConfig securityConfig;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationRepository authenticationRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private AccountRouteRepository accountRouteRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private AccountUtils accountUtils;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);
        Account account = authenticationRepository.findByUsername(username);
        if (account == null) {
            log.warn("Username not found: {}", username);
            throw new UsernameNotFoundException("User not found with username: " + username);
        }
        return account;
    }

    public Object login(LoginRequest loginRequest) {
        try {
            if(loginRequest == null || loginRequest.getPassword().isEmpty() || loginRequest.getUsername().isEmpty()){
                throw new BadCredentialsException("Vui lòng điền đầy đủ thông tin đăng nhập!");
            }
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(),
                    loginRequest.getPassword()
            ));
            Account account = authenticationRepository.findByUsername(loginRequest.getUsername());
            if (account == null || !securityConfig.passwordEncoder().matches(loginRequest.getPassword(), account.getPassword())) {
                throw new BadCredentialsException("Tên đăng nhập hoặc mật khẩu không đúng!");
            }
            if(!account.getStatus().equals(AccountStatus.HOAT_DONG)){
                throw new AuthenticationServiceException("Tài khoản bạn đã bị khóa!");
            }

            String token = tokenService.generateToken(account);

            if (account instanceof Staff){
                StaffReponse staffReponse = new StaffReponse();
                staffReponse.setAccountId(account.getAccountId());
                staffReponse.setUsername(account.getUsername());
                staffReponse.setEmail(account.getEmail());
                staffReponse.setPhone(account.getPhone());
                staffReponse.setName(account.getName());
                staffReponse.setRole(account.getRole());
                staffReponse.setStatus(account.getStatus());
                staffReponse.setCreatedAt(account.getCreatedAt());
                staffReponse.setStaffCode(((Staff) account).getStaffCode());
                staffReponse.setDepartment(((Staff) account).getDepartment());
                staffReponse.setLocation(((Staff) account).getLocation());
                staffReponse.setToken(token);
                return staffReponse;
            } else if (account instanceof Customer){
                CustomerReponse customerReponse = new CustomerReponse();
                customerReponse.setAccountId(account.getAccountId());
                customerReponse.setUsername(account.getUsername());
                customerReponse.setEmail(account.getEmail());
                customerReponse.setPhone(account.getPhone());
                customerReponse.setName(account.getName());
                customerReponse.setRole(account.getRole());
                customerReponse.setStatus(account.getStatus());
                customerReponse.setCustomerCode(((Customer) account).getCustomerCode());
                customerReponse.setAddress(((Customer) account).getAddress());
                customerReponse.setSource(((Customer) account).getSource());
                customerReponse.setToken(token);
                return customerReponse;
            }
        } catch (AuthenticationException e) {
            throw new BadCredentialsException("Tên đăng nhập hoặc mật khẩu không đúng!");
        }
        return null;
    }

    public Staff registerStaff(RegisterStaffRequest registerRequest) {
        if (authenticationRepository.findByUsername(registerRequest.getUsername()) != null){
            throw new BadCredentialsException("Tên đăng nhập bị trùng, vui lòng chọn một tên khác!");
        }
        Staff staff = new Staff();
        staff.setUsername(registerRequest.getUsername());
        staff.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        staff.setEmail(registerRequest.getEmail());
        staff.setPhone(registerRequest.getPhone());
        staff.setName(registerRequest.getName());
        staff.setRole(registerRequest.getRole());
        staff.setStatus(AccountStatus.HOAT_DONG);
        staff.setCreatedAt(LocalDateTime.now());
        staff.setStaffCode(generateStaffCode());
        staff.setDepartment(registerRequest.getDepartment());
        staff.setLocation(registerRequest.getLocation());

        List<Long> routeIds = registerRequest.getRouteIds();
        if (routeIds != null && !routeIds.isEmpty()) {
            for (Long routeId : routeIds) {
                Route route = routeRepository.findById(routeId)
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tuyến hàng này!"));
                AccountRoute accountRoute = new AccountRoute();
                accountRoute.setAccount(staff);
                accountRoute.setRoute(route);
                staff = authenticationRepository.save(staff);
                accountRouteRepository.save(accountRoute);
            }
        }
        return staff;
    }

    public Customer registerCustomer(RegisterCustomerRequest registerRequest) {
        if (authenticationRepository.findByUsername(registerRequest.getUsername()) != null){
            throw new BadCredentialsException("Tên đăng nhập bị trùng, vui lòng chọn một tên khác!");
        }

        if (authenticationRepository.findByPhone(registerRequest.getPhone()) != null){
            throw new BadCredentialsException("Số điện thoại bị trùng, vui lòng chọn một số khác!");
        }

        if (authenticationRepository.findByEmail(registerRequest.getEmail()) != null){
            throw new BadCredentialsException("Email bị trùng, vui lòng chọn một email khác!");
        }

        Customer customer = new Customer();
        customer.setUsername(registerRequest.getUsername());
        customer.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        customer.setEmail(registerRequest.getEmail());
        customer.setPhone(registerRequest.getPhone());
        customer.setName(registerRequest.getName());
        customer.setRole(AccountRoles.CUSTOMER);
        customer.setStatus(AccountStatus.HOAT_DONG);
        customer.setCreatedAt(LocalDateTime.now());
        customer.setCustomerCode(generateCustomerCode());
        customer.setAddress(registerRequest.getAddress());
        customer.setSource(registerRequest.getSource());
        customer = authenticationRepository.save(customer);

        return customer;
    }

    public String generateCustomerCode() {
        String customerCode;
        do {
            customerCode = "KH-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        } while (customerRepository.existsByCustomerCode(customerCode));
        return customerCode;
    }

    public String generateStaffCode() {
        String customerCode;
        do {
            customerCode = "NV-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        } while (staffRepository.existsByStaffCode(customerCode));
        return customerCode;
    }

    public void logout() {
        SecurityContextHolder.clearContext();
    }

//    public Account findOrCreateGoogleAccount(String email, String name) {
//        Account account = authenticationRepository.findByUsername(email);
//        if (account != null) {
//            System.out.println("Account found: " + email);
//            return account;
//        } else {
//            Customer customer = new Customer();
//            customer.setUsername(email);
//            customer.setName(name);
//            customer.setPassword(""); // Hoặc mã hóa nếu cần
//            customer.setRole(AccountRoles.CUSTOMER);
//            customer.setStatus(AccountStatus.HOAT_DONG);
//            customer.setCreatedAt(LocalDateTime.now());
//            customer.setCustomerCode(generateCustomerCode());
//            customer.setAddress("Default Address");
//            authenticationRepository.save(customer);
//            System.out.println("New customer saved: " + email); // Log để check DB
//            return customer;
//        }
//    }

    public List<Customer> searchCustomersByPhoneOrName(String keyword) {
        return customerRepository.findByPhoneOrNameContainingAndStaffId(keyword, accountUtils.getAccountCurrent().getAccountId());
    }

    public Customer registerCustomerByStaff(RegisterCustomerRequest registerRequest) {
        if (authenticationRepository.findByUsername(registerRequest.getUsername()) != null){
            throw new BadCredentialsException("Tên đăng nhập bị trùng, vui lòng chọn một tên khác!");
        }

        if (authenticationRepository.findByPhone(registerRequest.getPhone()) != null){
            throw new BadCredentialsException("Số điện thoại bị trùng, vui lòng chọn một số khác!");
        }

        if (authenticationRepository.findByEmail(registerRequest.getEmail()) != null){
            throw new BadCredentialsException("Email bị trùng, vui lòng chọn một email khác!");
        }

        Customer customer = new Customer();
        customer.setUsername(registerRequest.getPhone());
        customer.setPassword(passwordEncoder.encode("123456"));
        customer.setEmail(registerRequest.getEmail());
        customer.setPhone(registerRequest.getPhone());
        customer.setName(registerRequest.getName());
        customer.setRole(AccountRoles.CUSTOMER);
        customer.setStatus(AccountStatus.HOAT_DONG);
        customer.setCreatedAt(LocalDateTime.now());
        customer.setCustomerCode(generateCustomerCode());
        customer.setAddress(registerRequest.getAddress());
        customer.setSource(registerRequest.getSource());
        customer.setStaffId(accountUtils.getAccountCurrent().getAccountId());
        customer = authenticationRepository.save(customer);

        return customer;
    }
}