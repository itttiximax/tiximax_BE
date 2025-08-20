package com.tiximax.txm.API;

import com.tiximax.txm.Entity.Warehouse;
import com.tiximax.txm.Model.WarehouseRequest;
import com.tiximax.txm.Service.WarehouseService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/warehouse")
@SecurityRequirement(name = "bearerAuth")

public class WarehouseController {

    @Autowired
    private WarehouseService warehouseService;

    @PostMapping("/create/{purchaseId}/{locationId}")
    public ResponseEntity<Warehouse> createWarehouseEntry(@PathVariable Long purchaseId, @PathVariable Long locationId, @RequestBody WarehouseRequest warehouseRequest) {
        Warehouse warehouse = warehouseService.createWarehouseEntry(purchaseId, locationId, warehouseRequest);
        return ResponseEntity.ok(warehouse);
    }

    @GetMapping("/by-tracking-code/{trackingCode}")
    public ResponseEntity<Warehouse> getWarehouseByTrackingCode(@PathVariable String trackingCode) {
        Warehouse warehouse = warehouseService.getWarehouseByTrackingCode(trackingCode);
        return ResponseEntity.ok(warehouse);
    }

    @GetMapping("/by-order-code/{orderCode}")
    public ResponseEntity<List<Warehouse>> getWarehousesByOrderCode(@PathVariable String orderCode) {
        List<Warehouse> warehouses = warehouseService.getWarehousesByOrderCode(orderCode);
        return ResponseEntity.ok(warehouses);
    }

    @GetMapping("/by-purchase-id/{purchaseId}")
    public ResponseEntity<List<Warehouse>> getWarehousesByPurchaseId(@PathVariable Long purchaseId) {
        List<Warehouse> warehouses = warehouseService.getWarehousesByPurchaseId(purchaseId);
        return ResponseEntity.ok(warehouses);
    }

}
