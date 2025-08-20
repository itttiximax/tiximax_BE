package com.tiximax.txm.Service;

import com.tiximax.txm.Config.SecurityConfig;
import com.tiximax.txm.Entity.Account;
import com.tiximax.txm.Entity.Customer;
import com.tiximax.txm.Entity.Staff;
import com.tiximax.txm.Enums.AccountRoles;
import com.tiximax.txm.Enums.AccountStatus;
import com.tiximax.txm.Model.*;
import com.tiximax.txm.Repository.AuthenticationRepository;
import com.tiximax.txm.Repository.CustomerRepository;
import com.tiximax.txm.Repository.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

            if (!account.getRole().equals(AccountRoles.KHACH_HANG)){
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
                staffReponse.setPosition(((Staff) account).getPosition());
                staffReponse.setLocation(((Staff) account).getLocation());
                staffReponse.setToken(token);
                return staffReponse;
            } else if (account.getRole().equals(AccountRoles.KHACH_HANG)){
                CustomerReponse customerReponse = new CustomerReponse();
                customerReponse.setAccountId(account.getAccountId());
                customerReponse.setUsername(account.getUsername());
                customerReponse.setEmail(account.getEmail());
                customerReponse.setPhone(account.getPhone());
                customerReponse.setName(account.getName());
                customerReponse.setRole(account.getRole());
                customerReponse.setStatus(account.getStatus());
                customerReponse.setCustomerCode(((Customer) account).getCustomerCode());
                customerReponse.setType(((Customer) account).getType());
                customerReponse.setAddress(((Customer) account).getAddress());
                customerReponse.setTaxCode(((Customer) account).getTaxCode());
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
        staff.setStaffCode("A");
        staff.setDepartment(registerRequest.getDepartment());
        staff.setPosition(registerRequest.getPosition());
        staff.setLocation(registerRequest.getLocation());
        return authenticationRepository.save(staff);
    }

    public Customer registerCustomer(RegisterCustomerRequest registerRequest) {
        if (authenticationRepository.findByUsername(registerRequest.getUsername()) != null){
            throw new BadCredentialsException("Tên đăng nhập bị trùng, vui lòng chọn một tên khác!");
        }
        Customer customer = new Customer();
        customer.setUsername(registerRequest.getUsername());
        customer.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        customer.setEmail(registerRequest.getEmail());
        customer.setPhone(registerRequest.getPhone());
        customer.setName(registerRequest.getName());
        customer.setRole(AccountRoles.KHACH_HANG);
        customer.setStatus(AccountStatus.HOAT_DONG);
        customer.setCreatedAt(LocalDateTime.now());
        customer.setCustomerCode(generateCustomerCode());
        customer.setType(registerRequest.getType());
        customer.setAddress(registerRequest.getAddress());
        customer.setTaxCode(registerRequest.getTaxCode());
        customer.setSource(registerRequest.getSource());
        return authenticationRepository.save(customer);
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
}