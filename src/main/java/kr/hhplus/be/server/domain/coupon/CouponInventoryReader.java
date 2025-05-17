package kr.hhplus.be.server.domain.coupon;

import java.time.LocalDateTime;

public interface CouponInventoryReader {
    boolean issue(Long couponId, Long userId);
    void release(Long couponId, Long userId);
    void initialize(Long couponId, int limitCount, LocalDateTime expirationAt);
}

