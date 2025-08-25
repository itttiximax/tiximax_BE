package com.tiximax.txm.API;

import com.tiximax.txm.Entity.Route;
import com.tiximax.txm.Model.RouteRequest;
import com.tiximax.txm.Service.RouteService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/routes")
@SecurityRequirement(name = "bearerAuth")
public class RouteController {

    @Autowired
    private RouteService routeService;

    @PostMapping
    public ResponseEntity<Route> createRoute(@RequestBody RouteRequest routeRequest) {
        Route route = routeService.createRoute(routeRequest);
        return ResponseEntity.ok(route);
    }

    @GetMapping("/{routeId}")
    public ResponseEntity<Route> getRouteById(@PathVariable Long routeId) {
        Route route = routeService.getRouteById(routeId);
        return ResponseEntity.ok(route);
    }

    @GetMapping
    public ResponseEntity<List<Route>> getAllRoutes() {
        List<Route> routes = routeService.getAllRoutes();
        return ResponseEntity.ok(routes);
    }

    @PutMapping("/{routeId}")
    public ResponseEntity<Route> updateRoute(@PathVariable Long routeId, @RequestBody RouteRequest routeRequest) {
        Route route = routeService.updateRoute(routeId, routeRequest);
        return ResponseEntity.ok(route);
    }

    @DeleteMapping("/{routeId}")
    public ResponseEntity<Void> deleteRoute(@PathVariable Long routeId) {
        routeService.deleteRoute(routeId);
        return ResponseEntity.noContent().build();
    }
}