package kr.hhplus.be.server.application.coupon;

import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.domain.coupon.UserCouponStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class CouponFacade {

    private final CouponService couponService;
    private final UserCouponService userCouponService;

    /**
     * 사용자에게 쿠폰을 발급합니다. (선착순 정책 포함)
     */
    public UserCoupon issueCoupon(Long userId, Long couponId) {
        // 1. 이미 쿠폰을 발급받은 경우 예외 처리
        userCouponService.validateNotDuplicated(userId, couponId);

        // 2. 쿠폰 유효성 검증 및 발급 수 증가
        Coupon coupon = couponService.issueCoupon(couponId);

        // 3. 사용자 쿠폰 생성 및 저장
        UserCoupon userCoupon = new UserCoupon(
                null,
                userId,
                coupon.id(),
                UserCouponStatus.ISSUED,
                LocalDateTime.now(),
                null
        );
        return userCouponService.save(userCoupon);
    }

    /**
     *쿠폰 단건조회
     * */
    public Coupon getCouponOrThrow(Long couponId) {
        return couponService.getCouponOrThrow(couponId);
    }
}