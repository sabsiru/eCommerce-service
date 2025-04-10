package kr.hhplus.be.server.application.payment;

import kr.hhplus.be.server.domain.payment.Payment;
import kr.hhplus.be.server.domain.payment.PaymentRepository;
import kr.hhplus.be.server.domain.payment.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
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
        // Payment.initiate 호출 시 PENDING 상태, id는 null
        Payment initiatedPayment = Payment.initiate(orderId, amount);
        // 실제 Repository에서 저장된 Payment는 id가 부여됨 (예: 1L)
        Payment savedPayment = new Payment(1L, orderId, amount, PaymentStatus.PENDING,
                initiatedPayment.createdAt(), initiatedPayment.updatedAt(),null);
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        // when
        Payment result = paymentService.initiatePayment(orderId, amount);

        // then
        assertEquals(1L, result.id());
        assertEquals(orderId, result.orderId());
        assertEquals(amount, result.amount());
        assertEquals(PaymentStatus.PENDING, result.status());
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void 결제_완료_테스트() throws InterruptedException {
        // given
        Long paymentId = 1L;
        // 결제 초기 Payment 객체, id가 1L로 부여된 상태
        Payment initiated = Payment.initiate(1L, 100_000);
        Payment pendingPayment = new Payment(paymentId, 1L, 100_000, PaymentStatus.PENDING,
                initiated.createdAt(), initiated.updatedAt(),null);
        // 결제 완료 호출로 COMPLETED 상태로 전환된 Payment 객체
        // (시간 차이를 보이기 위해 Thread.sleep 활용)
        Thread.sleep(10);
        Payment completedPayment = pendingPayment.complete();
        Payment savedAfterComplete = new Payment(paymentId, 1L, 100_000, PaymentStatus.COMPLETED,
                pendingPayment.createdAt(), completedPayment.updatedAt(),null);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedAfterComplete);

        // when
        Payment result = paymentService.completePayment(paymentId);

        // then
        assertEquals(paymentId, result.id());
        assertEquals(PaymentStatus.COMPLETED, result.status());
        assertTrue(result.updatedAt().isAfter(pendingPayment.updatedAt()),
                "업데이트 시각이 갱신되어야 합니다.");
        verify(paymentRepository, times(1)).findById(paymentId);
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void 결제_환불_테스트() throws InterruptedException {
        // given
        Long paymentId = 1L;
        // 최초 Payment 생성 및 저장 - PENDING 상태
        Payment initiated = Payment.initiate(1L, 100_000);
        Payment pendingPayment = new Payment(paymentId, 1L, 100_000, PaymentStatus.PENDING,
                initiated.createdAt(), initiated.updatedAt(),null);
        // 결제 완료 상태로 전환
        Payment completedPayment = pendingPayment.complete();
        // 잠시 대기
        Thread.sleep(10);
        // 환불 처리: 결제 완료(PAID) 상태에서 refund() 호출 시 REFUND 상태로 전환
        Payment refundedPayment = completedPayment.refund();
        Payment savedAfterRefund = new Payment(paymentId, 1L, 100_000, PaymentStatus.REFUND,
                pendingPayment.createdAt(), refundedPayment.updatedAt(),null);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(completedPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedAfterRefund);

        // when
        Payment result = paymentService.refundPayment(paymentId);

        // then
        assertEquals(paymentId, result.id());
        assertEquals(PaymentStatus.REFUND, result.status());
        verify(paymentRepository, times(1)).findById(paymentId);
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void 결제_초기화_쿠폰포함_테스트() {
        // given
        Long orderId = 1L;
        int amount = 100_000;
        Long couponId = 500L;
        // Payment.initiate 호출 시 PENDING 상태, id는 null, couponId는 입력된 값이 기록됨
        Payment initiatedPayment = Payment.initiate(orderId, amount, couponId);
        // 실제 Repository에서 저장된 Payment는 id가 부여됨 (예: 1L)
        Payment savedPayment = new Payment(1L, orderId, amount, PaymentStatus.PENDING,
                initiatedPayment.createdAt(), initiatedPayment.updatedAt(), couponId);
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        // when
        Payment result = paymentService.initiatePayment(orderId, amount, couponId);

        // then
        assertEquals(1L, result.id());
        assertEquals(orderId, result.orderId());
        assertEquals(amount, result.amount());
        assertEquals(PaymentStatus.PENDING, result.status());
        assertEquals(couponId, result.couponId());
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }
}