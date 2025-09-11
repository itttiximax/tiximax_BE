package com.tiximax.txm.API;

import com.tiximax.txm.Entity.Purchases;
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

//    @PostMapping("/same-shop")
//    public ResponseEntity<Purchases> addPurchase(@RequestParam String orderCode, @RequestBody List<String> purchaseCode) {
//        Purchases purchase = purchaseService.createPurchase(orderCode, purchaseCode);
//        return ResponseEntity.ok(purchase);
//    }

    @PostMapping("/same-shop")
    public ResponseEntity<Purchases> addPurchase(@RequestParam String orderCode, @RequestBody PurchaseRequest purchaseRequest) {
        Purchases purchase = purchaseService.createPurchase(orderCode, purchaseRequest);
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

//    @PutMapping("/{purchaseId}")
//    public ResponseEntity<Purchases> updatePurchase(@PathVariable Long id, @RequestBody Purchases purchaseDetails) {
//        Purchases updatedPurchase = purchaseService.updatePurchase(id, purchaseDetails);
//        return ResponseEntity.ok(updatedPurchase);
//    }

    @DeleteMapping("/{purchaseId}")
    public ResponseEntity<Void> deletePurchase(@PathVariable Long purchaseId) {
        purchaseService.deletePurchase(purchaseId);
        return ResponseEntity.noContent().build();
    }

}
