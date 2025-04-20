package kr.hhplus.be.server.domain.coupon;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CouponTest {

    @Test
    void 쿠폰_생성_테스트() {
        Coupon coupon = Coupon.create(
                "테스트 쿠폰",
                20,
                2000,
                LocalDateTime.now().plusDays(7),
                100
        );

        assertNotNull(coupon);
        assertEquals("테스트 쿠폰", coupon.getName());
        assertEquals(20, coupon.getDiscountRate());
        assertEquals(2000, coupon.getMaxDiscountAmount());
        assertEquals(CouponStatus.ACTIVE, coupon.getStatus());
        assertEquals(100, coupon.getLimitCount());
        assertEquals(0, coupon.getIssuedCount());
    }

    @Test
    void 쿠폰_증가_카운트_테스트() {
        Coupon coupon = Coupon.create("테스트 쿠폰", 20, 2000, LocalDateTime.now().plusDays(7), 5);

        Coupon result = coupon.increaseIssuedCount();
        result = result.increaseIssuedCount();
        result = result.increaseIssuedCount();

        assertEquals(3, result.getIssuedCount());
        assertEquals(CouponStatus.ACTIVE, result.getStatus());
    }

    @Test
    void 쿠폰_증가_카운트_최대_도달_테스트() {
        Coupon coupon = Coupon.create("테스트 쿠폰", 20, 2000, LocalDateTime.now().plusDays(7), 3);

        Coupon result = coupon.increaseIssuedCount();
        result = result.increaseIssuedCount();
        assertEquals(2, result.getIssuedCount());
        assertEquals(CouponStatus.ACTIVE, result.getStatus());

        result = result.increaseIssuedCount();
        assertEquals(3, result.getIssuedCount());
        assertEquals(CouponStatus.EXPIRED, result.getStatus());

        IllegalStateException ex = assertThrows(IllegalStateException.class, result::increaseIssuedCount);
        assertEquals("쿠폰 발급 수량이 모두 소진되었습니다.", ex.getMessage());
    }

    @Test
    void 쿠폰_할인_금액_계산_테스트() {
        Coupon coupon = Coupon.create("테스트 쿠폰", 30, 5000, LocalDateTime.now().plusDays(7), 100);

        int orderAmount = 20000;
        int discount = coupon.calculateDiscountAmount(orderAmount);
        assertEquals(5000, discount);

        orderAmount = 10000;
        discount = coupon.calculateDiscountAmount(orderAmount);
        assertEquals(3000, discount);
    }

    @Test
    void 쿠폰_만료_테스트() {
        Coupon coupon = Coupon.create("테스트 쿠폰", 20, 2000, LocalDateTime.now().minusDays(1), 100);

        Coupon expired = coupon.expire();
        assertEquals(CouponStatus.EXPIRED, expired.getStatus());
    }
}
