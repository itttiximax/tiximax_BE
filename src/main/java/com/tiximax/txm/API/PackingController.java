package com.tiximax.txm.API;

import com.tiximax.txm.Entity.Packing;
import com.tiximax.txm.Model.PackingRequest;
import com.tiximax.txm.Service.PackingService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/packings")
@SecurityRequirement(name = "bearerAuth")

public class PackingController {

//    @Autowired
//    private PackingService packingService;
//
//    @PostMapping
//    public ResponseEntity<Packing> createPacking(@RequestBody PackingRequest request) {
//        Packing packing = packingService.createPacking(request);
//        return ResponseEntity.ok(packing);
//    }
//
//    @GetMapping("/{id}")
//    public ResponseEntity<Packing> getPackingById(@PathVariable Long id) {
//        Packing packing = packingService.getPackingById(id);
//        return ResponseEntity.ok(packing);
//    }
//
//    @GetMapping
//    public ResponseEntity<List<Packing>> getAllPackings() {
//        List<Packing> packings = packingService.getAllPackings();
//        return ResponseEntity.ok(packings);
//    }
//
//    @PutMapping("/{id}/packing-list")
//    public ResponseEntity<Packing> updatePackingList(
//            @PathVariable Long id,
//            @RequestBody List<String> packingList) {
//        Packing packing = packingService.updatePackingList(id, packingList);
//        return ResponseEntity.ok(packing);
//    }

}
