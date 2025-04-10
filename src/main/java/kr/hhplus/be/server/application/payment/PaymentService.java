package kr.hhplus.be.server.application.payment;

import kr.hhplus.be.server.domain.payment.Payment;
import kr.hhplus.be.server.domain.payment.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public Payment initiatePayment(long orderId, int amount) {
        Payment payment = Payment.initiate(orderId, amount);
        return paymentRepository.save(payment);
    }

    // 쿠폰 사용하여 결제 초기화 (couponId를 포함)
    public Payment initiatePayment(long orderId, int amount, Long couponId) {
        Payment payment = Payment.initiate(orderId, amount, couponId);
        return paymentRepository.save(payment);
    }

    public Payment completePayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다. paymentId=" + paymentId));
        Payment updatedPayment = payment.complete();
        return paymentRepository.save(updatedPayment);
    }


    public Payment refundPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다. paymentId=" + paymentId));
        Payment updatedPayment = payment.refund();
        return paymentRepository.save(updatedPayment);
    }
}