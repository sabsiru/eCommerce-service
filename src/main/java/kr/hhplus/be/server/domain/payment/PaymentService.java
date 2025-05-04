package kr.hhplus.be.server.domain.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public Payment initiateWithoutCoupon(Long orderId, int amount) {
        Payment payment = Payment.withoutCoupon(orderId, amount);
        return paymentRepository.save(payment);
    }

    public Payment initiateWithCoupon(Long orderId, int amount, Long couponId) {
        Payment payment = Payment.withCoupon(orderId, amount, couponId);
        return paymentRepository.save(payment);
    }

    public Payment complete(Long paymentId) {
        Payment payment = getPaymentForCompleteOrThrow(paymentId);
        payment.complete();

        return paymentRepository.save(payment);
    }

    public Payment refund(Long paymentId) {
        Payment payment = getPaymentForRefundOrThrow(paymentId);
        payment.refund();

        return paymentRepository.save(payment);
    }

    public Payment getPaymentForRefundOrThrow(Long paymentId) {
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "결제를 찾을 수 없습니다. paymentId=" + paymentId));

        if (payment.getStatus() == PaymentStatus.REFUND) {
            throw new IllegalStateException(
                    "이미 환불된 주문입니다. paymentId=" + paymentId);
        }
        return payment;
    }

    public Payment getPaymentForCompleteOrThrow(Long paymentId) {
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "결제를 찾을 수 없습니다. paymentId=" + paymentId));

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            throw new IllegalStateException(
                    "이미 결제된 주문입니다. paymentId=" + paymentId);
        }
        return payment;
    }
}