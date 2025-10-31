package com.tiximax.txm.API;

import com.tiximax.txm.Entity.Purchases;
import com.tiximax.txm.Model.PendingShipmentPurchase;
import com.tiximax.txm.Model.PurchaseDetail;
import com.tiximax.txm.Model.PurchaseRequest;
import com.tiximax.txm.Service.PurchaseService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    public ResponseEntity<Purchases> addPurchase(@RequestParam String orderCode, @RequestBody PurchaseRequest purchaseRequest) {
        Purchases purchase = purchaseService.createPurchase(orderCode, purchaseRequest);
        return ResponseEntity.ok(purchase);
    }
    @PostMapping("auction/add")
    public ResponseEntity<Purchases> addAuction(@RequestParam String orderCode, @RequestBody PurchaseRequest purchaseRequest) {
        Purchases purchase = purchaseService.createAuction(orderCode, purchaseRequest);
        return ResponseEntity.ok(purchase);
    }   

    @GetMapping("/{page}/{size}/paging")
    public ResponseEntity<Page<Purchases>> getAllPurchases(@PathVariable int page, @PathVariable int size) {
        Sort sort = Sort.by("purchaseTime").descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Purchases> purchasesPage = (Page<Purchases>) purchaseService.getAllPurchases(pageable);
        return ResponseEntity.ok(purchasesPage);
    }

    @GetMapping("/{purchaseId}")
    public ResponseEntity<PurchaseDetail> getPurchaseById(@PathVariable Long purchaseId) {
        PurchaseDetail purchaseDetail = purchaseService.getPurchaseById(purchaseId);
        return ResponseEntity.ok(purchaseDetail);
    }

    @PutMapping("/shipment/{purchaseId}")
    public ResponseEntity<Purchases> updateShipment(
            @PathVariable Long purchaseId,
            @RequestBody String shipmentCode) {

        Purchases updated = purchaseService.updateShipmentForPurchase(purchaseId, shipmentCode);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{purchaseId}")
    public ResponseEntity<Void> deletePurchase(@PathVariable Long purchaseId) {
        purchaseService.deletePurchase(purchaseId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pending-shipment")
    public ResponseEntity<List<PendingShipmentPurchase>> getPurchasesWithPendingShipment() {
        List<PendingShipmentPurchase> result = purchaseService.getPurchasesWithPendingShipment();
        return ResponseEntity.ok(result);
    }

}
