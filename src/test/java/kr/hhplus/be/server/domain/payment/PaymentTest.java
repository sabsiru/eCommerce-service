package kr.hhplus.be.server.domain.payment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentTest {

    @Test
    void 결제_생성_쿠폰_사용_없음() {
        // when
        Payment payment = Payment.withoutCoupon(1L, 10000);

        // then
        assertNotNull(payment);
        assertEquals(1L, payment.getOrderId());
        assertEquals(10000, payment.getAmount());
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        assertNull(payment.getCouponId());
    }

    @Test
    void 결제_생성_쿠폰_사용_포함() {
        // when
        Payment payment = Payment.create(1L, 10000, 55L);

        // then
        assertNotNull(payment);
        assertEquals(1L, payment.getOrderId());
        assertEquals(10000, payment.getAmount());
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        assertEquals(55L, payment.getCouponId());
    }

    @Test
    void 결제_완료_정상_상태변경() {
        // given
        Payment payment = Payment.withoutCoupon(1L, 10000);

        // when
        payment.complete();

        // then
        assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
    }

    @Test
    void 결제_완료_실패_이미_완료된_경우() {
        // given
        Payment payment = Payment.withoutCoupon(1L, 10000);
        payment.complete();

        // when & then
        IllegalStateException ex = assertThrows(IllegalStateException.class, payment::complete);
        assertEquals("결제가 이미 완료되었습니다.", ex.getMessage());
    }

    @Test
    void 환불_정상_상태변경() {
        // given
        Payment payment = Payment.withoutCoupon(1L, 10000);
        payment.complete();

        // when
        payment.refund();

        // then
        assertEquals(PaymentStatus.REFUND, payment.getStatus());
    }

    @Test
    void 환불_실패_완료되지_않은_상태() {
        // given
        Payment payment = Payment.withoutCoupon(1L, 10000);

        // when & then
        IllegalStateException ex = assertThrows(IllegalStateException.class, payment::refund);
        assertEquals("결제가 완료되지 않은 주문입니다.", ex.getMessage());
    }
}