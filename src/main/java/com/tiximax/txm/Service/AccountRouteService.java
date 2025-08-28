package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.Account;
import com.tiximax.txm.Entity.AccountRoute;
import com.tiximax.txm.Entity.Route;
import com.tiximax.txm.Repository.AccountRouteRepository;
import com.tiximax.txm.Repository.AuthenticationRepository;
import com.tiximax.txm.Repository.RouteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccountRouteService {

    @Autowired
    private AccountRouteRepository accountRouteRepository;

    @Autowired
    private AuthenticationRepository authenticationRepository;

    @Autowired
    private RouteRepository routeRepository;

    public AccountRoute createAccountRoute(Long accountId, Long routeId) {
        Account account = authenticationRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản với ID: " + accountId));
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tuyến hàng với ID: " + routeId));

        AccountRoute accountRoute = new AccountRoute();
        accountRoute.setAccount(account);
        accountRoute.setRoute(route);
        return accountRouteRepository.save(accountRoute);
    }

    public AccountRoute getAccountRouteById(Long accountRouteId) {
        return accountRouteRepository.findById(accountRouteId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy AccountRoute với ID: " + accountRouteId));
    }

    public Page<AccountRoute> getAllAccountRoutes(Pageable pageable) {
        return accountRouteRepository.findAll(pageable);
    }

    public List<AccountRoute> getAllAccountRoutes() {
        return accountRouteRepository.findAll();
    }

    public AccountRoute updateAccountRoute(Long accountRouteId, Long accountId, Long routeId) {
        AccountRoute accountRoute = accountRouteRepository.findById(accountRouteId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy AccountRoute với ID: " + accountRouteId));

        Account account = authenticationRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản với ID: " + accountId));
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tuyến hàng với ID: " + routeId));

        accountRoute.setAccount(account);
        accountRoute.setRoute(route);
        return accountRouteRepository.save(accountRoute);
    }

    public void deleteAccountRoute(Long accountRouteId) {
        if (!accountRouteRepository.existsById(accountRouteId)) {
            throw new IllegalArgumentException("Không tìm thấy AccountRoute với ID: " + accountRouteId);
        }
        accountRouteRepository.deleteById(accountRouteId);
    }
}
