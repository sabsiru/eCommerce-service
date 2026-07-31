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


    public Payment create(Long orderId, int amount, Long couponId) {
        Payment payment = Payment.create(orderId, amount, couponId);
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

}