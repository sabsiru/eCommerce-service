package kr.hhplus.be.server.application.coupon;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.CouponService;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Transactional
public class CouponFacade {

    private final CouponService couponService;

    /**
     * 사용자에게 쿠폰을 발급합니다. (선착순 정책 포함)
     */
    public UserCoupon issueCoupon(Long userId, Long couponId) {
        return couponService.issueCoupon(userId, couponId);
    }

    /**
     *쿠폰 단건조회
     * */
    public Coupon getCouponOrThrow(Long couponId) {
        return couponService.getCouponOrThrow(couponId);
    }
}