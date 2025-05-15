package kr.hhplus.be.server.application.coupon;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.CouponService;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CouponFacade {

    private final CouponService couponService;

    public UserCoupon issue(Long userId, Long couponId) {
        return couponService.issue(userId, couponId);
    }

    @Transactional
    public Coupon getCouponOrThrow(Long couponId) {
        return couponService.getCouponOrThrow(couponId);
    }
}