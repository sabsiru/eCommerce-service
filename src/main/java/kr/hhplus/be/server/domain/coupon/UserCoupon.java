package kr.hhplus.be.server.domain.coupon;

import java.time.LocalDateTime;

public record UserCoupon(
        Long id,
        Long userId,
        Long couponId,
        UserCouponStatus status,
        LocalDateTime issuedAt,
        LocalDateTime usedAt
) {
    public UserCoupon use() {
        if (status != UserCouponStatus.ISSUED) {
            throw new IllegalStateException("사용 할 수 없는 쿠폰 입니다.");
        }
        return new UserCoupon(id, userId, couponId, UserCouponStatus.USED, issuedAt, LocalDateTime.now());
    }

    public UserCoupon refund() {
        if (status != UserCouponStatus.USED) {
            throw new IllegalStateException("환불 가능한 쿠폰이 아닙니다.");
        }
        return new UserCoupon(id, userId, couponId, UserCouponStatus.ISSUED, issuedAt, null);
    }
}
