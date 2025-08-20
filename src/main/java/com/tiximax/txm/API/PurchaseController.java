package com.tiximax.txm.API;

import com.tiximax.txm.Entity.Purchases;
import com.tiximax.txm.Service.PurchaseService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/purchases")
@SecurityRequirement(name = "bearerAuth")

public class PurchaseController {

    @Autowired
    private PurchaseService purchaseService;

    @PostMapping("/add")
    public ResponseEntity<Purchases> addPurchase(@RequestParam String orderCode, @RequestBody List<String> trackingCodes) {
        Purchases purchase = purchaseService.createPurchase(orderCode, trackingCodes);
        return ResponseEntity.ok(purchase);
    }

}
