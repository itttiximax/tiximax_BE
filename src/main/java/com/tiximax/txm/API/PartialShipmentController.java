package com.tiximax.txm.API;

import com.tiximax.txm.Entity.Packing;
import com.tiximax.txm.Entity.PartialShipment;
import com.tiximax.txm.Entity.Payment;
import com.tiximax.txm.Model.ShipmentCodesRequest;
import com.tiximax.txm.Service.PartialShipmentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;


@RestController
@CrossOrigin
@RequestMapping("/partial-shipment")
@SecurityRequirement(name = "bearerAuth")

public class PartialShipmentController {

    @Autowired
    private PartialShipmentService partialShipmentService;

    @PostMapping("/partial-shipment/{isUseBalance}/{bankId}/{customerVoucherId}")
    public ResponseEntity<Payment> createPartialShipment(@RequestBody ShipmentCodesRequest selectedTrackingCode,
                                                                    @PathVariable boolean isUseBalance,
                                                                    @PathVariable Long bankId,
                                                                    @PathVariable BigDecimal priceShipDos,
                                                                    @RequestParam(required = false) Long customerVoucherId) {
        List<PartialShipment> partial = partialShipmentService.createPartialShipment(selectedTrackingCode, isUseBalance, bankId, priceShipDos, customerVoucherId);
        Payment payment = partial.get(0).getPayment();                                                             
        return ResponseEntity.ok(payment);
    }
    @GetMapping("/{id}")
    public ResponseEntity<PartialShipment> getPartialShipmentById(@PathVariable Long id) {
    Optional<PartialShipment> partialShipment = partialShipmentService.getById(id);
    return partialShipment.map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
}
}
