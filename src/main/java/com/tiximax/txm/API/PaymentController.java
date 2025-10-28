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
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@CrossOrigin
@RequestMapping("/payments")
@SecurityRequirement(name = "bearerAuth")

public class PaymentController {

    @Autowired
    private PaymentService paymentService;

//    @PostMapping("{orderCode}/{depositPercent}/{isUseBalance}")
//    public ResponseEntity<Payment> createPayment(@PathVariable String orderCode,
//                                                 @PathVariable Integer depositPercent,
//                                                 @PathVariable boolean isUseBalance) {
//        Payment createdPayment = paymentService.createPayment(orderCode, depositPercent, isUseBalance);
//        return ResponseEntity.ok(createdPayment);
//    }

    @GetMapping("/order/{orderCode}")
    public ResponseEntity<List<Payment>> getPaymentsByOrderId(@PathVariable String orderCode) {
        List<Payment> payments = paymentService.getPaymentsByOrderCode(orderCode);
        return ResponseEntity.ok(payments);
    }

    @PostMapping("/merged/{depositPercent}/{isUseBalance}")
    public ResponseEntity<Payment> createMergedPayment(@RequestBody Set<String> orderCodes,
                                                       @PathVariable Integer depositPercent,
                                                       @PathVariable boolean isUseBalance) {
        Payment createdPayment = paymentService.createMergedPayment(orderCodes, depositPercent, isUseBalance);
        return ResponseEntity.ok(createdPayment);
    }

    @PostMapping("/merged-shipping/{isUseBalance}/{customerVoucherId}")
    public ResponseEntity<Payment> createMergedPaymentShipping(@RequestBody Set<String> orderCodes,
                                                               @PathVariable boolean isUseBalance,
                                                               @PathVariable(required = false) Long customerVoucherId) {
        Payment createdPayment = paymentService.createMergedPaymentShipping(orderCodes, isUseBalance, customerVoucherId);
        return ResponseEntity.ok(createdPayment);
    }

    @PutMapping("/confirm/{paymentCode}")
    public ResponseEntity<Payment> confirmPayment(@PathVariable String paymentCode) {
        Payment confirmedPayment = paymentService.confirmedPayment(paymentCode);
        return ResponseEntity.ok(confirmedPayment);
    }

    @PutMapping("/confirm-shipping/{paymentCode}")
    public ResponseEntity<Payment> confirmPaymentShipping(@PathVariable String paymentCode) {
        Payment confirmedPayment = paymentService.confirmedPaymentShipment(paymentCode);
        return ResponseEntity.ok(confirmedPayment);
    }

    @GetMapping("/auction")
    public ResponseEntity<List<Payment>> getAuctionPayment() {
        List<Payment> confirmedPayment = paymentService.getPaymentByStaffandStatus();
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
