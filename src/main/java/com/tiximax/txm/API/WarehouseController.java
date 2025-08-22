package com.tiximax.txm.API;

import com.tiximax.txm.Entity.Warehouse;
import com.tiximax.txm.Model.WarehouseRequest;
import com.tiximax.txm.Service.WarehouseService;
import com.tiximax.txm.Utils.AccountUtils;
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

    @Autowired
    private AccountUtils accountUtils;

    @PostMapping("/{purchaseId}/{locationId}")
    public ResponseEntity<List<Warehouse>> createWarehouseEntry(@PathVariable Long purchaseId, @PathVariable Long locationId, @RequestBody WarehouseRequest warehouseRequest) {
        List<Warehouse> warehouses = warehouseService.createWarehouseEntry(purchaseId, locationId, warehouseRequest);
        return ResponseEntity.ok(warehouses);
    }

    @GetMapping("/orderlink/{orderLinkId}/in-warehouse")
    public ResponseEntity<Boolean> isOrderLinkInWarehouse(@PathVariable Long orderLinkId) {
        return ResponseEntity.ok(warehouseService.isOrderLinkInWarehouse(orderLinkId));
    }

    @GetMapping("/purchase/{purchaseId}/fully-received")
    public ResponseEntity<Boolean> isPurchaseFullyReceived(@PathVariable Long purchaseId) {
        return ResponseEntity.ok(warehouseService.isPurchaseFullyReceived(purchaseId));
    }

}
