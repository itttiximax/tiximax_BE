package com.tiximax.txm.API;

import com.tiximax.txm.Entity.Domestic;
import com.tiximax.txm.Model.CreateDomesticRequest;
import com.tiximax.txm.Service.DomesticService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/domestics")
@SecurityRequirement(name = "bearerAuth")

public class DomesticController {

    @Autowired
    private DomesticService domesticService;

    @PostMapping("/received")
    public ResponseEntity<Domestic> createDomesticForWarehousing(@RequestBody CreateDomesticRequest request) {
        if (request == null || request.getPackingCode().isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }
        Domestic domestic = domesticService.createDomesticForWarehousing(request.getPackingCode(), request.getNote());
        return ResponseEntity.ok(domestic);
    }

    @GetMapping("/ready-for-delivery/{page}/{size}")
    public ResponseEntity<List<Map<String, Object>>> getReadyForDeliveryOrders(@PathVariable int page, @PathVariable int size) {
        Sort sort = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        List<Map<String, Object>> result = domesticService.getReadyForDeliveryOrders(pageable);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/transfer-to-customer")
    public ResponseEntity<List<Domestic>> transferToCustomer(){
        List<Domestic> domestic = domesticService.TransferToCustomer();
        return ResponseEntity.ok(domestic);
    }

}
