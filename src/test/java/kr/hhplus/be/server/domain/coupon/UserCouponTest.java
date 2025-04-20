package kr.hhplus.be.server.domain.coupon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UserCouponTest {

    @Test
    @DisplayName("쿠폰 사용 성공 - 상태가 ISSUED인 경우")
    void 쿠폰_사용_성공() {
        // given
        UserCoupon userCoupon = new UserCoupon(
                1L,
                1L,
                1L,
                UserCouponStatus.ISSUED,
                LocalDateTime.now().minusDays(1),
                null
        );

        // when
        userCoupon.use();

        // then
        assertEquals(UserCouponStatus.USED, userCoupon.getStatus());
        assertNotNull(userCoupon.getUsedAt());
    }

    @Test
    @DisplayName("쿠폰 사용 실패 - 상태가 ISSUED가 아닌 경우")
    void 쿠폰_사용_실패_상태_비정상() {
        // given
        UserCoupon alreadyUsed = new UserCoupon(
                1L,
                1L,
                1L,
                UserCouponStatus.USED,
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1)
        );

        // when & then
        IllegalStateException exception = assertThrows(IllegalStateException.class, alreadyUsed::use);
        assertEquals("사용 할 수 없는 쿠폰 입니다.", exception.getMessage());
    }

    @Test
    void 쿠폰_환불_성공_테스트() {
        // Arrange: USED 상태의 쿠폰 생성
        Long id = 1L;
        Long userId = 100L;
        Long couponId = 500L;
        LocalDateTime issuedAt = LocalDateTime.now().minusDays(10);
        LocalDateTime usedAt = LocalDateTime.now().minusDays(1);

        UserCoupon usedCoupon = new UserCoupon(id, userId, couponId, UserCouponStatus.USED, issuedAt, usedAt);

        // Act: refund() 호출
        usedCoupon.refund();

        // Assert: 반환된 쿠폰의 상태가 ISSUED로 변경되고, usedAt이 null이어야 함
        assertEquals(UserCouponStatus.ISSUED, usedCoupon.getStatus(), "환불 후 쿠폰 상태는 ISSUED여야 합니다.");
        assertEquals(id, usedCoupon.getId());
        assertEquals(userId, usedCoupon.getUserId());
        assertEquals(couponId, usedCoupon.getCouponId());
        assertEquals(issuedAt, usedCoupon.getIssuedAt());
        assertNull(usedCoupon.getUsedAt(), "환불된 쿠폰의 usedAt은 null이어야 합니다.");
    }

    @Test
    void 쿠폰_환불_실패_테스트() {
        // Arrange: ISSUED 상태의 쿠폰 생성 (USED 상태가 아님)
        Long id = 1L;
        Long userId = 100L;
        Long couponId = 500L;
        LocalDateTime issuedAt = LocalDateTime.now().minusDays(10);

        UserCoupon issuedCoupon = new UserCoupon(id, userId, couponId, UserCouponStatus.ISSUED, issuedAt, null);

        // Act & Assert: refund() 호출 시 예외 발생을 검증
        IllegalStateException exception = assertThrows(IllegalStateException.class, issuedCoupon::refund);
        assertEquals("환불 가능한 쿠폰이 아닙니다.", exception.getMessage());
    }
}