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

    @PostMapping("/partial-shipment/{isUseBalance}/{bankId}/{customerVoucherId}")
    public ResponseEntity<List<PartialShipment>> createPartialShipment(@RequestBody TrackingCodesRequest selectedTrackingCode,
                                                                    @PathVariable boolean isUseBalance,
                                                                    @PathVariable Long bankId,
                                                                    @RequestParam(required = false) Long customerVoucherId) {
        List<PartialShipment> partial = partialShipmentService.createPartialShipment(selectedTrackingCode, isUseBalance, bankId, customerVoucherId);
        return ResponseEntity.ok(partial);
    }
}
