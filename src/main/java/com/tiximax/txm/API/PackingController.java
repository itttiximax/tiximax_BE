//package com.tiximax.txm.API;
//
//import com.tiximax.txm.Entity.Packing;
//import com.tiximax.txm.Service.PackingService;
//import io.swagger.v3.oas.annotations.security.SecurityRequirement;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@CrossOrigin
//@RequestMapping("/packings")
//@SecurityRequirement(name = "bearerAuth")
//
//public class PackingController {
//
//    @Autowired
//    private PackingService packingService;
//
//    @PostMapping("/from-orders")
//    public ResponseEntity<Packing> createFromOrders(@RequestBody List<Long> orderIds) {
//        return ResponseEntity.ok(packingService.createPackingFromOrders(orderIds));
//    }
//
//    @GetMapping
//    public ResponseEntity<List<Packing>> getAll() {
//        return ResponseEntity.ok(packingService.getAll());
//    }
//
//    @GetMapping("/{id}")
//    public ResponseEntity<Packing> getById(@PathVariable Long id) {
//        return packingService.getById(id)
//                .map(ResponseEntity::ok)
//                .orElse(ResponseEntity.notFound().build());
//    }
//
//    @PutMapping("/{id}")
//    public ResponseEntity<Packing> update(@PathVariable Long id, @RequestBody Packing updated) {
//        return ResponseEntity.ok(packingService.updatePacking(id, updated));
//    }
//
//    @DeleteMapping("/{id}")
//    public ResponseEntity<Void> delete(@PathVariable Long id) {
//        packingService.deletePacking(id);
//        return ResponseEntity.noContent().build();
//    }
//
//}
