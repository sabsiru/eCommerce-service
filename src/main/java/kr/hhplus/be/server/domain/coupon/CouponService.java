package kr.hhplus.be.server.domain.coupon;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final CouponInventoryReader couponInventoryReader;

    @Transactional
    public Coupon create(String name, int discountRate,
                         int maxDiscountAmount, LocalDateTime expirationAt, int limitCount) {
        Coupon coupon = Coupon.create(
                name,
                discountRate,
                maxDiscountAmount,
                expirationAt,
                limitCount
        );

        Coupon saved = couponRepository.save(coupon);

        couponInventoryReader.initialize(saved.getId(), limitCount, expirationAt);

        return saved;
    }

    public Coupon getCouponOrThrow(Long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다. couponId=" + couponId));
    }

    @Transactional
    public UserCoupon issue(Long userId, Long couponId) {
        couponInventoryReader.issue(couponId, userId);

        try {
            UserCoupon userCoupon = UserCoupon.issue(userId, couponId);
            return userCouponRepository.save(userCoupon);
        } catch (Exception e) {
            couponInventoryReader.release(couponId, userId);
            throw e;
        }
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