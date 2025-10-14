package com.tiximax.txm.Service;

import com.tiximax.txm.Config.SecurityConfig;
import com.tiximax.txm.Entity.*;
import com.tiximax.txm.Enums.AccountRoles;
import com.tiximax.txm.Enums.AccountStatus;
import com.tiximax.txm.Enums.OrderStatus;
import com.tiximax.txm.Model.*;
import com.tiximax.txm.Repository.*;
import com.tiximax.txm.Utils.AccountUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.json.JSONObject;

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
    private OrdersRepository ordersRepository;

    @Autowired
    private AccountUtils accountUtils;

    
    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseAnonKey;

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

    public Page<Staff> getAllStaff(Pageable pageable) {
        return staffRepository.findAll(pageable);
    }

    public Page<Customer> getAllCustomers(Pageable pageable) {
        return customerRepository.findAll(pageable);
    }

    public Page<Customer> getCustomersByStaff(Pageable pageable) {
        Long staffId = accountUtils.getAccountCurrent().getAccountId();
        return customerRepository.findByStaffId(staffId, pageable);
    }

    public Page<Staff> getSaleAndLeadSaleStaff(Pageable pageable) {
        List<AccountRoles> roles = Arrays.asList(AccountRoles.STAFF_SALE, AccountRoles.LEAD_SALE);
        Page<Account> accounts = authenticationRepository.findByRoleIn(roles, pageable);
        List<Staff> staffList = accounts.getContent().stream()
                .filter(account -> account instanceof Staff)
                .map(account -> (Staff) account)
                .collect(Collectors.toList());
        return new PageImpl<>(staffList, pageable, accounts.getTotalElements());
    }

    public Page<Staff> getSalesInSameRoute(Pageable pageable) {
        Account currentAccount = accountUtils.getAccountCurrent();
        if (currentAccount.getRole() != AccountRoles.LEAD_SALE) {
            throw new SecurityException("Vị trí của bạn không được phép cho chức năng này!");
        }

        List<AccountRoute> leadSaleRoutes = accountRouteRepository.findByAccount_AccountId(currentAccount.getAccountId());
        Set<Long> routeIds = leadSaleRoutes.stream()
                .map(AccountRoute::getRoute)
                .map(Route::getRouteId)
                .collect(Collectors.toSet());

        List<AccountRoute> allAccountRoutes = accountRouteRepository.findByRoute_RouteIdIn(routeIds);

        List<Staff> sales = allAccountRoutes.stream()
                .map(AccountRoute::getAccount)
                .filter(account -> account instanceof Staff && account.getRole() == AccountRoles.STAFF_SALE)
                .map(account -> (Staff) account)
                .distinct()
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), sales.size());
        List<Staff> pagedSales = sales.subList(start, end);
        return new PageImpl<>(pagedSales, pageable, sales.size());
    }

    public List<SaleStats> getSalesStatsInSameRoute(String timeFrame, Integer year, Integer month, Integer week) {
        Account currentAccount = accountUtils.getAccountCurrent();
        if (currentAccount.getRole() != AccountRoles.LEAD_SALE) {
            throw new SecurityException("Chỉ LEAD_SALE được phép truy cập dữ liệu này!");
        }

        // Lấy danh sách route của LEAD_SALE
        List<AccountRoute> leadSaleRoutes = accountRouteRepository.findByAccount_AccountId(currentAccount.getAccountId());
        Set<Long> routeIds = leadSaleRoutes.stream()
                .map(AccountRoute::getRoute)
                .map(Route::getRouteId)
                .collect(Collectors.toSet());

        // Lấy danh sách nhân viên SALE và LEAD_SALE trong cùng route
        List<AccountRoute> allAccountRoutes = accountRouteRepository.findByRoute_RouteIdIn(routeIds);
        List<Staff> sales = allAccountRoutes.stream()
                .map(AccountRoute::getAccount)
                .filter(account -> account instanceof Staff &&
                        (account.getRole() == AccountRoles.STAFF_SALE || account.getRole() == AccountRoles.LEAD_SALE))
                .map(account -> (Staff) account)
                .distinct()
                .collect(Collectors.toList());

        // Xác định khoảng thời gian
        LocalDateTime startDate = null;
        LocalDateTime endDate = null;

        if (timeFrame != null && year != null && month != null) {
            if (timeFrame.equalsIgnoreCase("MONTH")) {
                if (month < 1 || month > 12) {
                    throw new IllegalArgumentException("Tháng phải từ 1 đến 12");
                }
                startDate = LocalDateTime.of(year, month, 1, 0, 0);
                endDate = startDate.plusMonths(1);
            } else if (timeFrame.equalsIgnoreCase("WEEK")) {
                if (month < 1 || month > 12) {
                    throw new IllegalArgumentException("Tháng phải từ 1 đến 12");
                }
                if (week == null || week < 1 || week > 5) {
                    throw new IllegalArgumentException("Tuần phải từ 1 đến 5");
                }
                LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
                startDate = firstDayOfMonth.plusWeeks(week - 1).atStartOfDay();
                endDate = startDate.plusWeeks(1);
            } else {
                throw new IllegalArgumentException("timeFrame không hợp lệ. Sử dụng MONTH hoặc WEEK");
            }
        } else if (timeFrame != null || year != null || month != null || week != null) {
            throw new IllegalArgumentException("Thiếu tham số bắt buộc: MONTH cần timeFrame, year, month; WEEK cần thêm week");
        }

        // Tính thống kê cho từng nhân viên
        List<SaleStats> statsList = new ArrayList<>();
        for (Staff sale : sales) {
            // Lấy danh sách đơn hàng
            List<Orders> saleOrders;
            if (startDate != null && endDate != null) {
                saleOrders = ordersRepository.findByStaff_AccountIdAndRoute_RouteIdInAndCreatedAtBetween(
                        sale.getAccountId(), routeIds, startDate, endDate);
            } else {
                saleOrders = ordersRepository.findByStaff_AccountIdAndRoute_RouteIdIn(sale.getAccountId(), routeIds);
            }

            // Tổng số đơn hàng
            long totalOrders = saleOrders.size();

            // Tổng doanh thu
            BigDecimal totalRevenue = saleOrders.stream()
                    .map(Orders::getFinalPriceOrder)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Số khách hàng duy nhất
            long uniqueCustomers = saleOrders.stream()
                    .map(Orders::getCustomer)
                    .map(Customer::getAccountId)
                    .distinct()
                    .count();

            // Số khách hàng mới
            long newCustomerCount;
            if (startDate != null && endDate != null) {
                newCustomerCount = customerRepository.countByStaffIdAndCreatedAtBetween(sale.getAccountId(), startDate, endDate);
            } else {
                newCustomerCount = customerRepository.countByStaffId(sale.getAccountId());
            }

            // Giá trị trung bình đơn hàng
            BigDecimal averageOrderValue = totalOrders > 0 ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;

            // Tạo SaleStats
            SaleStats stats = new SaleStats();
            stats.setSaleId(sale.getAccountId());
            stats.setSaleName(sale.getName());
            stats.setTotalOrders(totalOrders);
            stats.setTotalRevenue(totalRevenue);
            stats.setUniqueCustomers(uniqueCustomers);
            stats.setNewCustomers(newCustomerCount);
            stats.setAverageOrderValue(averageOrderValue);

            statsList.add(stats);
        }

        // Sắp xếp theo tổng số đơn hàng giảm dần
        statsList.sort(Comparator.comparingLong(SaleStats::getTotalOrders).reversed());

        return statsList;
    }

    public Account findOrCreateGoogleAccount(String email, String name) {
        Account account = authenticationRepository.findByUsername(email);
        if (account != null) {
            System.out.println("Account found: " + email);
            return account;
        } else {
            Customer customer = new Customer();
            customer.setUsername(email);
            customer.setName(name);
            customer.setPassword(""); // Hoặc mã hóa nếu cần
            customer.setRole(AccountRoles.CUSTOMER);
            customer.setStatus(AccountStatus.HOAT_DONG);
            customer.setCreatedAt(LocalDateTime.now());
            customer.setCustomerCode(generateCustomerCode());
            customer.setAddress("Default Address");
            authenticationRepository.save(customer);
            System.out.println("New customer saved: " + email); // Log để check DB
            return customer;
        }
    }
    public Account verifyAndSaveUser(String email, String name) {
    // 1️⃣ Kiểm tra nếu Account đã tồn tại
    Account existingAccount = authenticationRepository.findByEmail(email);
    if (existingAccount != null) {
        return existingAccount;
    }
       Customer customer = new Customer();
        customer.setUsername(email);
        customer.setPassword(email);
        customer.setEmail(email);
        customer.setPhone("00000000000");
        customer.setName(name);
        customer.setRole(AccountRoles.CUSTOMER);
        customer.setStatus(AccountStatus.HOAT_DONG);
        customer.setCreatedAt(LocalDateTime.now());
        customer.setCustomerCode(generateCustomerCode());
        customer.setAddress(null);
        customer.setSource("Google");
        customer = authenticationRepository.save(customer);
        return customer;
}
}
