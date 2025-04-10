package kr.hhplus.be.server.application.coupon;

import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CouponService {

    private final CouponRepository couponRepository;

    /**
     * 쿠폰 단건 조회
     */
    public Coupon getCouponOrThrow(Long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다. couponId=" + couponId));
    }

    /**
     * 쿠폰 발급 처리 (수량 증가 및 상태 만료 처리 포함)
     */
    public Coupon issueCoupon(Long couponId) {
        Coupon coupon = getCouponOrThrow(couponId);
        Coupon updated = coupon.increaseIssuedCount();

        return couponRepository.save(updated);
    }
}