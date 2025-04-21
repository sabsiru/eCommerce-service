package kr.hhplus.be.server.domain.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public Payment initiateWithoutCoupon(Long orderId, int amount) {
        Payment payment = Payment.withoutCoupon(orderId, amount);  // 정적 팩토리 메서드 사용
        return paymentRepository.save(payment);
    }

    public Payment initiateWithCoupon(Long orderId, int amount, Long couponId) {
        Payment payment = Payment.withCoupon(orderId, amount, couponId);
        return paymentRepository.save(payment);
    }

    public Payment completePayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다. paymentId=" + paymentId));

        payment.complete();
        return paymentRepository.save(payment);
    }

    public Payment refundPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다. paymentId=" + paymentId));

        payment.refund();
        return paymentRepository.save(payment);
    }
}