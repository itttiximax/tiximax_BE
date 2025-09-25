package com.tiximax.txm.API;

import com.tiximax.txm.Entity.Packing;
import com.tiximax.txm.Model.AssignFlightRequest;
import com.tiximax.txm.Model.PackingEligibleOrder;
import com.tiximax.txm.Model.PackingInWarehouse;
import com.tiximax.txm.Model.PackingRequest;
import com.tiximax.txm.Service.PackingService;
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
@RequestMapping("/packings")
@SecurityRequirement(name = "bearerAuth")

public class PackingController {

    @Autowired
    private PackingService packingService;

    @GetMapping("/eligible-orders/{page}/{size}")
    public ResponseEntity<Page<PackingEligibleOrder>> getEligibleOrdersForPacking(@PathVariable int page, @PathVariable int size) {
        Sort sort = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<PackingEligibleOrder> eligibleOrdersPage = packingService.getEligibleOrdersForPacking(pageable);
        return ResponseEntity.ok(eligibleOrdersPage);
    }

    @PostMapping
    public ResponseEntity<Packing> createPacking(@RequestBody PackingRequest request) {
        Packing packing = packingService.createPacking(request);
        return ResponseEntity.ok(packing);
    }

    @GetMapping("/in-warehouse/{page}/{size}")
    public ResponseEntity<Page<PackingInWarehouse>> getPackingsInWarehouse(@PathVariable int page, @PathVariable int size) {
        Sort sort = Sort.by("packedDate").descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<PackingInWarehouse> packingsPage = packingService.getPackingsInWarehouse(pageable);
        return ResponseEntity.ok(packingsPage);
    }

    @GetMapping("/awaiting-flight/{page}/{size}")
    public ResponseEntity<Page<Packing>> getPackingsAwaitingFlight(@PathVariable int page, @PathVariable int size) {
        Sort sort = Sort.by("packedDate").descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Packing> packingsPage = packingService.getPackingsAwaitingFlight(pageable);
        return ResponseEntity.ok(packingsPage);
    }

    @PutMapping("/assign-flight")
    public ResponseEntity<String> assignFlightCode(@RequestBody AssignFlightRequest request) {
        List<Long> packingIds = request.getPackingIds();
        String flightCode = request.getFlightCode();
        if (packingIds == null || packingIds.isEmpty() || flightCode == null || flightCode.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        packingService.assignFlightCode(packingIds, flightCode);
        return ResponseEntity.ok("Flight code assigned successfully");
    }

}
