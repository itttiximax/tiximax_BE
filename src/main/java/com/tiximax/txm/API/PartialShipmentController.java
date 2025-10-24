package com.tiximax.txm.API;

import com.tiximax.txm.Entity.PartialShipment;
import com.tiximax.txm.Model.TrackingCodesRequest;
import com.tiximax.txm.Service.PartialShipmentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/partial-shipment")
@SecurityRequirement(name = "bearerAuth")

public class PartialShipmentController {

    @Autowired
    private PartialShipmentService partialShipmentService;

    @PostMapping("/partial-shipment/{orderId}")
    public ResponseEntity<PartialShipment> createPartialShipment(@PathVariable Long orderId, @RequestBody TrackingCodesRequest selectedTrackingCodes) {
        PartialShipment partial = partialShipmentService.createPartialShipment(orderId, selectedTrackingCodes);
        return ResponseEntity.ok(partial);
    }
}
