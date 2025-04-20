package kr.hhplus.be.server.interfaces.payment;

import kr.hhplus.be.server.application.payment.PaymentFacade;
import kr.hhplus.be.server.domain.payment.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentFacade paymentFacade;

    @PatchMapping("/{orderId}/pay")
    public ResponseEntity<Payment> completePayment(
            @PathVariable Long orderId,
            @RequestBody PaymentRequest request
    ) {
        Payment payment = paymentFacade.processPayment(orderId, request.getPaymentAmount());
        return ResponseEntity.ok(payment);
    }

    @PatchMapping("/{paymentId}/refund")
    public ResponseEntity<PaymentResponse> refund(@PathVariable Long paymentId) {
        Payment payment = paymentFacade.processRefund(paymentId);
        return ResponseEntity.ok(PaymentResponse.from(payment));
    }
}