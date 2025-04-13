package kr.hhplus.be.server.domain.coupon;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CouponTest {

    @Test
    void 쿠폰_생성_테스트() {
        // Arrange
        Long id = 1L;
        String name = "테스트 쿠폰";
        int discountRate = 20; // 20%
        int maxDiscountAmount = 2000;
        LocalDateTime expirationAt = LocalDateTime.now().plusDays(7);
        int limitCount = 100;

        // Act
        Coupon coupon = Coupon.create(id, name, discountRate, maxDiscountAmount, expirationAt, limitCount);

        // Assert
        assertNotNull(coupon);
        assertEquals(id, coupon.id());
        assertEquals(name, coupon.name());
        assertEquals(discountRate, coupon.discountRate());
        assertEquals(maxDiscountAmount, coupon.maxDiscountAmount());
        assertEquals(CouponStatus.ACTIVE, coupon.status());
        assertEquals(expirationAt, coupon.expirationAt());
        assertEquals(limitCount, coupon.limitCount());
        assertEquals(0, coupon.issuedCount());
    }

    @Test
    void 쿠폰_증가_카운트_테스트() {
        // Arrange
        Long id = 1L;
        String name = "테스트 쿠폰";
        int discountRate = 20; // 20%
        int maxDiscountAmount = 2000;
        LocalDateTime expirationAt = LocalDateTime.now().plusDays(7);
        int limitCount = 5; // 제한 수량이 5
        Coupon coupon = Coupon.create(id, name, discountRate, maxDiscountAmount, expirationAt, limitCount);

        // Act: 3회 증가 (5회 한도 전이므로 상태는 ACTIVE)
        Coupon couponAfterIncrease = coupon.increaseIssuedCount();
        couponAfterIncrease = couponAfterIncrease.increaseIssuedCount();
        couponAfterIncrease = couponAfterIncrease.increaseIssuedCount();

        // Assert
        assertEquals(3, couponAfterIncrease.issuedCount());
        assertEquals(CouponStatus.ACTIVE, couponAfterIncrease.status());
    }

    @Test
    void 쿠폰_증가_카운트_최대_도달_테스트() {
        // Arrange
        Long id = 1L;
        String name = "테스트 쿠폰";
        int discountRate = 20; // 20%
        int maxDiscountAmount = 2000;
        LocalDateTime expirationAt = LocalDateTime.now().plusDays(7);
        int limitCount = 3; // 제한 수량 3
        Coupon coupon = Coupon.create(id, name, discountRate, maxDiscountAmount, expirationAt, limitCount);

        // Act: 2회 증가 후 상태는 ACTIVE
        Coupon couponAfterIncrease = coupon.increaseIssuedCount();
        couponAfterIncrease = couponAfterIncrease.increaseIssuedCount();

        assertEquals(2, couponAfterIncrease.issuedCount());
        assertEquals(CouponStatus.ACTIVE, couponAfterIncrease.status());

        // 3회 증가 시 한도 도달하므로 상태가 EXPIRED로 변경되어야 함
        Coupon couponAtLimit = couponAfterIncrease.increaseIssuedCount();
        assertEquals(3, couponAtLimit.issuedCount());
        assertEquals(CouponStatus.EXPIRED, couponAtLimit.status());

        // 한도를 초과하면 예외 발생
        Exception exception = assertThrows(IllegalStateException.class, () -> couponAtLimit.increaseIssuedCount());
        assertEquals("쿠폰 발급 수량이 모두 소진되었습니다.", exception.getMessage());
    }

    @Test
    void 쿠폰_할인_금액_계산_테스트() {
        // Arrange
        // 예: 할인율 30%, 최대 할인액 5000원
        Coupon coupon = new Coupon(
                1L,
                "테스트 쿠폰",
                30,
                5000,
                CouponStatus.ACTIVE,
                LocalDateTime.now().plusDays(7),
                LocalDateTime.now(),
                100,
                0
        );

        int orderAmount = 20000; // 30% of 20000 = 6000, 하지만 최대 5000원 적용되어야 함

        // Act
        int calculatedDiscount = coupon.calculateDiscountAmount(orderAmount);

        // Assert
        assertEquals(5000, calculatedDiscount);

        // 주문 금액이 낮은 경우, 계산된 할인액이 최대 할인액보다 작게 나와야 함
        orderAmount = 10000; // 30% of 10000 = 3000, 3000 < 5000
        calculatedDiscount = coupon.calculateDiscountAmount(orderAmount);
        assertEquals(3000, calculatedDiscount);
    }

    @Test
    void 쿠폰_만료_테스트() {
        // Arrange
        Long id = 1L;
        String name = "테스트 쿠폰";
        int discountRate = 20;
        int maxDiscountAmount = 2000;
        LocalDateTime expirationAt = LocalDateTime.now().minusDays(1); // 이미 만료된 날짜
        int limitCount = 100;
        Coupon coupon = Coupon.create(id, name, discountRate, maxDiscountAmount, expirationAt, limitCount);

        // Act
        Coupon expiredCoupon = coupon.expire();

        // Assert
        assertEquals(CouponStatus.EXPIRED, expiredCoupon.status());
    }
}