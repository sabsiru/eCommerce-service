package kr.hhplus.be.server.domain.coupon;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    public Coupon getCouponOrThrow(Long couponId) {
        return couponRepository.findByIdForUpdate(couponId)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다. couponId=" + couponId));
    }

    public UserCoupon issueCoupon(Long userId, Long couponId) {
        Coupon coupon = getCouponOrThrow(couponId);
        Coupon updated = coupon.increaseIssuedCount();
        couponRepository.save(updated);

        UserCoupon userCoupon = UserCoupon.issue(userId, couponId);

        try {
            return userCouponRepository.save(userCoupon);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("이미 발급받은 쿠폰입니다.");
        }
    }


    public UserCoupon getById(Long userCouponId) {
        return userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 쿠폰을 찾을 수 없습니다. userCouponId=" + userCouponId));
    }

    public List<UserCoupon> findByUserId(Long userId) {
        return userCouponRepository.findAllByUserId(userId);
    }


    public UserCoupon useCoupon(Long userCouponId) {
        UserCoupon userCoupon = getById(userCouponId);
        userCoupon.use();
        return userCoupon;
    }

    /**
     * 쿠폰 환불 처리
     */
    public UserCoupon refundCoupon(Long userCouponId) {
        UserCoupon userCoupon = getById(userCouponId);
        userCoupon.refund();
        return userCoupon;
    }

}