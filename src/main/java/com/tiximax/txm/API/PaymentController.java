package com.tiximax.txm.API;

import com.tiximax.txm.Entity.Payment;
import com.tiximax.txm.Service.PaymentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@CrossOrigin
@RequestMapping("/payments")
@SecurityRequirement(name = "bearerAuth")

public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    // Create a new Payment
    @PostMapping("{orderCode}")
    public ResponseEntity<Payment> createPayment(@PathVariable String orderCode) {
        Payment createdPayment = paymentService.createPayment(orderCode);
        return ResponseEntity.ok(createdPayment);
    }

    // Get all Payments by orderId
    @GetMapping("/order/{orderCode}")
    public ResponseEntity<List<Payment>> getPaymentsByOrderId(@PathVariable String orderCode) {
        List<Payment> payments = paymentService.getPaymentsByOrderCode(orderCode);
        return ResponseEntity.ok(payments);
    }

    @PostMapping("/shipping/{orderCode}")
    public ResponseEntity<Payment> createShippingPayment(@PathVariable String orderCode) {
        try {
            Payment createdShippingPayment = paymentService.createShippingPayment(orderCode);
            return ResponseEntity.ok(createdShippingPayment);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // Get a single Payment by ID
    @GetMapping("/{paymentCode}")
    public ResponseEntity<Payment> getPaymentById(@PathVariable String paymentCode) {
        Optional<Payment> payment = paymentService.getPaymentByCode(paymentCode);
        return payment.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Update an existing Payment
//    @PutMapping("/{paymentCode}")
//    public ResponseEntity<Payment> updatePayment(@PathVariable String paymentCode, @RequestBody Payment payment) {
//        try {
//            Payment updatedPayment = paymentService.updatePayment(paymentCode, payment);
//            return ResponseEntity.ok(updatedPayment);
//        } catch (RuntimeException e) {
//            return ResponseEntity.notFound().build();
//        }
//    }

//    @DeleteMapping("/{paymentCode}")
//    public ResponseEntity<Void> deletePayment(@PathVariable String paymentCode) {
//        try {
//            paymentService.deletePayment(paymentCode);
//            return ResponseEntity.noContent().build();
//        } catch (RuntimeException e) {
//            return ResponseEntity.notFound().build();
//        }
//    }

    @PutMapping("/confirm/{paymentCode}")
    public ResponseEntity<Payment> confirmPayment(@PathVariable String paymentCode) {
        Payment confirmedPayment = paymentService.confirmedPayment(paymentCode);
        return ResponseEntity.ok(confirmedPayment);
    }

}
