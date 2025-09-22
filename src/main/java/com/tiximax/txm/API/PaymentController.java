package com.tiximax.txm.API;

import com.tiximax.txm.Entity.Payment;
import com.tiximax.txm.Service.PaymentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@RestController
@CrossOrigin
@RequestMapping("/payments")
@SecurityRequirement(name = "bearerAuth")

public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("{orderCode}")
    public ResponseEntity<Payment> createPayment(@PathVariable String orderCode) {
        Payment createdPayment = paymentService.createPayment(orderCode);
        return ResponseEntity.ok(createdPayment);
    }

    @GetMapping("/order/{orderCode}")
    public ResponseEntity<List<Payment>> getPaymentsByOrderId(@PathVariable String orderCode) {
        List<Payment> payments = paymentService.getPaymentsByOrderCode(orderCode);
        return ResponseEntity.ok(payments);
    }

    @PostMapping("/merged")
    public ResponseEntity<Payment> createMergedPayment(@RequestBody Set<String> orderCodes) {
        Payment createdPayment = paymentService.createMergedPayment(orderCodes);
        return ResponseEntity.ok(createdPayment);
    }

    @PutMapping("/confirm/{paymentCode}")
    public ResponseEntity<Payment> confirmPayment(@PathVariable String paymentCode) {
        Payment confirmedPayment = paymentService.confirmedPayment(paymentCode);
        return ResponseEntity.ok(confirmedPayment);
    }

    @GetMapping("code/{paymentCode}")
    public ResponseEntity<Payment> getPaymentById(@PathVariable String paymentCode) {
        Optional<Payment> payment = paymentService.getPaymentByCode(paymentCode);
        return payment.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("id/{paymentId}")
    public ResponseEntity<Optional<Payment>> getPaymentsByOrderId(@PathVariable Long paymentId) {
        Optional<Payment> payment = paymentService.getPaymentsById(paymentId);
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/{orderId}/pending")
    public ResponseEntity<Payment> getPendingPaymentByOrderId(@PathVariable Long orderId) {
        Optional<Payment> payment = paymentService.getPendingPaymentByOrderId(orderId);
        return payment.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

}
