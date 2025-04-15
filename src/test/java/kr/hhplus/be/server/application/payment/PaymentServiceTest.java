package kr.hhplus.be.server.application.payment;

import kr.hhplus.be.server.domain.payment.Payment;
import kr.hhplus.be.server.domain.payment.PaymentRepository;
import kr.hhplus.be.server.domain.payment.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @InjectMocks
    private PaymentService paymentService;

    @Mock
    private PaymentRepository paymentRepository;

    @Test
    void 결제_초기화_테스트() {
        // given
        Long orderId = 1L;
        int amount = 100_000;
        Payment saved = Payment.withoutCoupon(orderId, amount); // 직접 생성
        // id 부여된 상태의 객체를 Stub으로 사용 (setter 없이도 가능)
        // → 미리 저장된 객체처럼 테스트
        when(paymentRepository.save(any(Payment.class))).thenReturn(saved);

        // when
        Payment result = paymentService.initiateWithoutCoupon(orderId, amount);

        // then
        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        assertEquals(amount, result.getAmount());
        assertEquals(PaymentStatus.PENDING, result.getStatus());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void 결제_초기화_쿠폰포함_테스트() {
        // given
        Long orderId = 1L;
        int amount = 100_000;
        Long couponId = 500L;

        // 저장 후 리턴될 객체(id가 부여된 상태로 가정)
        Payment savedPayment = Payment.withCoupon(orderId, amount, couponId);
        // 테스트에서 id를 setter 없이 설정하려면 builder 사용을 고려하거나 설계 변경 필요
        // 테스트에서는 저장 후 객체의 값만 검증하면 되므로 id는 검증하지 않아도 무방

        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        // when
        Payment result = paymentService.initiateWithCoupon(orderId, amount, couponId);

        // then
        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        assertEquals(amount, result.getAmount());
        assertEquals(PaymentStatus.PENDING, result.getStatus());
        assertEquals(couponId, result.getCouponId());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void 결제_완료_테스트() {
        // given
        Long paymentId = 1L;
        Payment pending = Payment.withoutCoupon(1L, 100_000);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(pending));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Payment result = paymentService.completePayment(paymentId);

        // then
        assertEquals(PaymentStatus.COMPLETED, result.getStatus());
        verify(paymentRepository).findById(paymentId);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void 결제_환불_테스트() {
        // given
        Long paymentId = 1L;
        Payment payment = Payment.withoutCoupon(1L, 100_000);
        payment.complete(); // 먼저 완료 상태로 전환

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Payment result = paymentService.refundPayment(paymentId);

        // then
        assertEquals(PaymentStatus.REFUND, result.getStatus());
        verify(paymentRepository).findById(paymentId);
        verify(paymentRepository).save(any(Payment.class));
    }
}