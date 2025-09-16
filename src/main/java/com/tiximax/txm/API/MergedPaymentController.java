package com.tiximax.txm.API;

import com.tiximax.txm.Entity.MergedPayment;
import com.tiximax.txm.Service.MergedPaymentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/merged-payment")
@SecurityRequirement(name = "bearerAuth")

public class MergedPaymentController {

    @Autowired
    private MergedPaymentService mergedPaymentService;

    @PostMapping
    public ResponseEntity<MergedPayment> createMergedPayment(@RequestBody List<String> orderCodes) {
        MergedPayment mergedPayment = mergedPaymentService.createMergedPayment(orderCodes);
        return ResponseEntity.ok(mergedPayment);
    }

}
