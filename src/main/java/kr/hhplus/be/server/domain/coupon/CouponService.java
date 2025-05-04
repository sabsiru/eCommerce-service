package kr.hhplus.be.server.domain.coupon;

import kr.hhplus.be.server.application.redis.DistributedLock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    public Coupon getCouponOrThrow(Long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다. couponId=" + couponId));
    }

    @Transactional
    @DistributedLock(key = "coupon:{#couponId}", waitTime = 2, leaseTime = 3)
    public UserCoupon issue(Long userId, Long couponId) {
        Coupon coupon = getCouponOrThrow(couponId);
        Coupon updated = coupon.increaseIssuedCount();
        couponRepository.save(updated);

        UserCoupon userCoupon = UserCoupon.issue(userId, couponId);
            return userCouponRepository.save(userCoupon);
    }

    public UserCoupon getById(Long userCouponId) {
        return userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 쿠폰을 찾을 수 없습니다. userCouponId=" + userCouponId));
    }

    @Transactional
    public List<UserCoupon> findByUserId(Long userId) {
        return userCouponRepository.findAllByUserId(userId);
    }

    @Transactional
    public UserCoupon use(Long userCouponId) {
        UserCoupon userCoupon = getById(userCouponId);
        userCoupon.use();
        return userCoupon;
    }

    @Transactional
    public UserCoupon refund(Long userCouponId) {
        UserCoupon userCoupon = getById(userCouponId);
        userCoupon.refund();
        return userCoupon;
    }

}