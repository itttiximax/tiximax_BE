package com.tiximax.txm.API;

import com.tiximax.txm.Entity.Orders;
import com.tiximax.txm.Entity.Warehouse;
import com.tiximax.txm.Model.WarehouseRequest;
import com.tiximax.txm.Model.WarehouseSummary;
import com.tiximax.txm.Service.WarehouseService;
import com.tiximax.txm.Utils.AccountUtils;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/warehouse")
@SecurityRequirement(name = "bearerAuth")

public class WarehouseController {

    @Autowired
    private WarehouseService warehouseService;

    @Autowired
    private AccountUtils accountUtils;

    @PostMapping("/shipment/{shipmentCode}")
    public ResponseEntity<Warehouse> createWarehouseEntryByShipmentCode(
            @PathVariable String shipmentCode,
            @RequestBody WarehouseRequest warehouseRequest) {
        Warehouse warehouse = warehouseService.createWarehouseEntryByShipmentCode(shipmentCode, warehouseRequest);
        return ResponseEntity.ok(warehouse);
    }

    @GetMapping("/{page}/{size}/ready-warehouses")
    public ResponseEntity<Page<WarehouseSummary>> getReadyWarehouses(@PathVariable int page, int size) {
        Sort sort = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<WarehouseSummary> warehousePage = warehouseService.getWarehousesForPacking(pageable);
        return ResponseEntity.ok(warehousePage);
    }

    @GetMapping("/totals")
    public ResponseEntity<Map<String, Double>> getWarehouseTotals() {
        return ResponseEntity.ok(warehouseService.calculateWarehouseTotals());
    }

}
