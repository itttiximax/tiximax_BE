package com.tiximax.txm.API;

import com.tiximax.txm.Entity.AccountRoute;
import com.tiximax.txm.Entity.Route;
import com.tiximax.txm.Service.AccountRouteService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/account-routes")
@SecurityRequirement(name = "bearerAuth")
public class AccountRouteController {

    @Autowired
    private AccountRouteService accountRouteService;

    @PostMapping
    public ResponseEntity<AccountRoute> createAccountRoute(
            @RequestParam Long accountId,
            @RequestParam Long routeId) {
        AccountRoute accountRoute = accountRouteService.createAccountRoute(accountId, routeId);
        return ResponseEntity.ok(accountRoute);
    }

    @GetMapping("/{accountRouteId}")
    public ResponseEntity<AccountRoute> getAccountRouteById(@PathVariable Long accountRouteId) {
        AccountRoute accountRoute = accountRouteService.getAccountRouteById(accountRouteId);
        return ResponseEntity.ok(accountRoute);
    }

//    @GetMapping("/{page}/{size}/paging")
//    public ResponseEntity<Page<AccountRoute>> getAllAccountRoutes(@PathVariable int page, int size) {
//        Sort sort = Sort.by("route.routeId").ascending();
//        Pageable pageable = PageRequest.of(page, size, sort);
//        Page<AccountRoute> accountRoutes = accountRouteService.getAllAccountRoutes(pageable);
//        return ResponseEntity.ok(accountRoutes);
//    }

//    @GetMapping
//    public ResponseEntity<List<AccountRoute>> getAllAccountRoutes() {
//        List<AccountRoute> accountRoutes = accountRouteService.getAllAccountRoutes();
//        return ResponseEntity.ok(accountRoutes);
//    }

//    @PutMapping("/{accountRouteId}")
//    public ResponseEntity<AccountRoute> updateAccountRoute(
//            @PathVariable Long accountRouteId,
//            @RequestParam Long accountId,
//            @RequestParam Long routeId) {
//        AccountRoute updatedAccountRoute = accountRouteService.updateAccountRoute(accountRouteId, accountId, routeId);
//        return ResponseEntity.ok(updatedAccountRoute);
//    }

    @DeleteMapping("/{accountRouteId}")
    public ResponseEntity<Void> deleteAccountRoute(@PathVariable Long accountRouteId) {
        accountRouteService.deleteAccountRoute(accountRouteId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-account")
    public List<Route> getRoutesByStaffId() {
        return accountRouteService.getByStaffId();
    }
}