package kr.hhplus.be.server.domain.payment;

import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.CouponStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PaymentTest {

    @Test
    void 결제_초기화_테스트() {
        // given
        Long orderId = 1L;
        int amount = 100_000;

        // when
        Payment payment = Payment.initiate(orderId, amount);

        // then
        assertEquals(orderId, payment.orderId());
        assertEquals(amount, payment.amount());
        assertEquals(PaymentStatus.PENDING, payment.status());
        assertNotNull(payment.createdAt());
        assertNotNull(payment.updatedAt());
    }

    @Test
    void 결제_완료_테스트() throws InterruptedException {
        // given
        Payment payment = Payment.initiate(1L, 100_000);
        LocalDateTime originalUpdatedAt = payment.updatedAt();

        // 잠시 대기하여 updatedAt 변경을 명확하게 하기 위해
        Thread.sleep(10);

        // when
        Payment completedPayment = payment.complete();

        // then
        assertEquals(PaymentStatus.COMPLETED, completedPayment.status());
        assertEquals(payment.orderId(), completedPayment.orderId());
        assertEquals(payment.amount(), completedPayment.amount());
        assertTrue(completedPayment.updatedAt().isAfter(originalUpdatedAt), "updatedAt은 갱신되어야 합니다.");
    }

    @Test
    void 결제_완료_실패_테스트() {
        // given
        Payment payment = Payment.initiate(1L, 100_000);
        // 먼저 결제 완료로 상태를 변경하여 PENDING 상태가 아니라는 것을 보장
        Payment completedPayment = payment.complete();

        // when & then
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> completedPayment.complete(),
                "이미 결제 완료된 상태에서 complete() 호출 시 예외가 발생해야 합니다.");
        assertEquals("결제가 이미 완료되었습니다.", e.getMessage());
    }

    @Test
    void 환불_성공_테스트() throws InterruptedException {
        // given
        Payment payment = Payment.initiate(1L, 100_000);
        Payment completedPayment = payment.complete();
        LocalDateTime originalUpdatedAt = completedPayment.updatedAt();

        // 잠시 대기하여 시간 차이를 확보
        Thread.sleep(10);

        // when
        Payment refundedPayment = completedPayment.refund();

        // then
        assertEquals(PaymentStatus.REFUND, refundedPayment.status());
        assertEquals(completedPayment.orderId(), refundedPayment.orderId());
        assertEquals(completedPayment.amount(), refundedPayment.amount());
        assertTrue(refundedPayment.updatedAt().isAfter(originalUpdatedAt), "환불 시 updatedAt이 갱신되어야 합니다.");
    }

    @Test
    void 환불_실패_테스트() {
        // given
        Payment payment = Payment.initiate(1L, 100_000);
        // 상태는 기본적으로 PENDING

        // when & then
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> payment.refund(),
                "PENDING 상태에서 refund() 호출 시 예외가 발생해야 합니다.");
        assertEquals("결제가 진행 중인 상태가 아닙니다.", ex.getMessage());
    }

    @Test
    void 쿠폰없이_초기화시_쿠폰아이디는_null이어야함() {
        long orderId = 1L;
        int amount = 1000;

        Payment payment = Payment.initiate(orderId, amount);

        assertNotNull(payment);
        assertEquals(orderId, payment.orderId());
        assertEquals(amount, payment.amount());
        assertEquals(PaymentStatus.PENDING, payment.status());
        // couponId가 null이어야 함
        assertNull(payment.couponId());
        // 생성 및 업데이트 시간은 null이 아님
        assertNotNull(payment.createdAt());
        assertNotNull(payment.updatedAt());
    }

    @Test
    void 쿠폰포함_초기화시_쿠폰아이디가_설정되어야함() {
        long orderId = 1L;
        int amount = 1000;
        Long couponId = 500L;

        Payment payment = Payment.initiate(orderId, amount, couponId);

        assertNotNull(payment);
        assertEquals(orderId, payment.orderId());
        assertEquals(amount, payment.amount());
        assertEquals(PaymentStatus.PENDING, payment.status());
        // couponId가 올바르게 저장되었는지 확인
        assertEquals(couponId, payment.couponId());
    }

    @Test
    void 결제완료시_쿠폰아이디가_유지되어야함() {
        long orderId = 1L;
        int amount = 1000;
        Long couponId = 500L;

        // couponId가 포함된 Payment 생성
        Payment initiatedPayment = Payment.initiate(orderId, amount, couponId);
        // Payment id는 null로 생성되었으므로, 임의의 값 부여 (실제 도메인에서는 DB에서 설정)
        Payment paymentWithId = new Payment(10L, initiatedPayment.orderId(), initiatedPayment.amount(),
                initiatedPayment.status(), initiatedPayment.createdAt(), initiatedPayment.updatedAt(), initiatedPayment.couponId());

        // complete() 호출
        Payment completedPayment = paymentWithId.complete();

        // couponId가 그대로 유지되는지 확인
        assertEquals(couponId, completedPayment.couponId());
        assertEquals(PaymentStatus.COMPLETED, completedPayment.status());
    }

    @Test
    void 환불시_쿠폰아이디가_유지되어야함() {
        long orderId = 1L;
        int amount = 1000;
        Long couponId = 500L;

        // couponId가 포함된 Payment 생성 및 complete()를 통해 상태 변경
        Payment initiatedPayment = Payment.initiate(orderId, amount, couponId);
        Payment paymentWithId = new Payment(10L, initiatedPayment.orderId(), initiatedPayment.amount(),
                initiatedPayment.status(), initiatedPayment.createdAt(), initiatedPayment.updatedAt(), initiatedPayment.couponId());
        Payment completedPayment = paymentWithId.complete();

        // refund() 호출
        Payment refundPayment = completedPayment.refund();

        // couponId가 그대로 유지되는지 확인
        assertEquals(couponId, refundPayment.couponId());
        assertEquals(PaymentStatus.REFUND, refundPayment.status());
    }


}