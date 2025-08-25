package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.Route;
import com.tiximax.txm.Model.RouteRequest;
import com.tiximax.txm.Repository.RouteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RouteService {

    @Autowired
    private RouteRepository routeRepository;

    public Route createRoute(RouteRequest routeRequest) {
        if (routeRequest.getName() == null || routeRequest.getName().isEmpty()) {
            throw new IllegalArgumentException("Tên tuyến đường không được để trống!");
        }
        if (routeRepository.existsByName(routeRequest.getName())) {
            throw new IllegalArgumentException("Tuyến đường với tên " + routeRequest.getName() + " đã tồn tại!");
        }

        Route route = new Route();
        route.setName(routeRequest.getName());
        route.setShipTime(routeRequest.getShipTime());
        route.setUnitShippingPrice(routeRequest.getUnitShippingPrice());
        route.setNote(routeRequest.getNote());

        return routeRepository.save(route);
    }

    public Route getRouteById(Long routeId) {
        return routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tuyến đường này!"));
    }

    public List<Route> getAllRoutes() {
        List<Route> routes = routeRepository.findAll();
        if (routes.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy tuyến đường nào!");
        }
        return routes;
    }

    public Route updateRoute(Long routeId, RouteRequest routeRequest) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tuyến đường này để cập nhật!"));

        if (routeRequest.getName() != null && !routeRequest.getName().isEmpty()) {
            if (!routeRequest.getName().equals(route.getName()) && routeRepository.existsByName(routeRequest.getName())) {
                throw new IllegalArgumentException("Tuyến đường với tên " + routeRequest.getName() + " đã tồn tại!");
            }
            route.setName(routeRequest.getName());
        }
        if (!routeRequest.getShipTime().isEmpty()) {
            route.setShipTime(routeRequest.getShipTime());
        }
        if (routeRequest.getUnitShippingPrice() != null) {
            route.setUnitShippingPrice(routeRequest.getUnitShippingPrice());
        }
        if (!routeRequest.getNote().isEmpty()) {
            route.setNote(routeRequest.getNote());
        }

        return routeRepository.save(route);
    }

    public void deleteRoute(Long routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tuyến đường với ID: " + routeId));
        if (!route.getOrders().isEmpty()) {
            throw new IllegalStateException("Không thể xóa tuyến đường vì đã có đơn hàng liên kết!");
        }
        routeRepository.delete(route);
    }
}