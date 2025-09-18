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

    @PostMapping("/shipment/{shipmentCode}")
    public ResponseEntity<Warehouse> createWarehouseEntryByShipmentCode(
            @PathVariable String shipmentCode,
            @RequestBody WarehouseRequest warehouseRequest) {
        Warehouse warehouse = warehouseService.createWarehouseEntryByShipmentCode(shipmentCode, warehouseRequest);
        return ResponseEntity.ok(warehouse);
    }

}
