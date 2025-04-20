package kr.hhplus.be.server.domain.coupon;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

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
    public UserCoupon issueCoupon(Long userId, Long couponId) {
        Optional<UserCoupon> existing = userCouponRepository.findByUserIdAndCouponId(userId, couponId);
        if (existing.isPresent()) {
            throw new IllegalStateException("이미 발급받은 쿠폰입니다.");
        }
        Coupon coupon = getCouponOrThrow(couponId);
        Coupon updated = coupon.increaseIssuedCount();
        couponRepository.save(updated);

        UserCoupon userCoupon = UserCoupon.issue(userId, coupon.getId());
        return userCouponRepository.save(userCoupon);
    }


    /**
     * ID로 사용자 쿠폰 조회 (없으면 예외)
     */
    public UserCoupon getById(Long userCouponId) {
        return userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 쿠폰을 찾을 수 없습니다. userCouponId=" + userCouponId));
    }

    /**
     * 사용자 ID로 모든 쿠폰 조회
     */
    public List<UserCoupon> findByUserId(Long userId) {
        return userCouponRepository.findAllByUserId(userId);
    }

    /**
     * 쿠폰 사용 처리
     */
    public UserCoupon useCoupon(Long userCouponId) {
        UserCoupon userCoupon = getById(userCouponId);
        userCoupon.use();  // 내부 상태만 변경
        return userCoupon;
    }

    /**
     * 쿠폰 환불 처리
     */
    public UserCoupon refundCoupon(Long userCouponId) {
        UserCoupon userCoupon = getById(userCouponId);
        userCoupon.refund();  // 내부 상태만 변경
        return userCoupon;
    }

}