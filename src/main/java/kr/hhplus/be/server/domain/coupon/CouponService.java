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
                         int maxDiscountAmount, String expirationAt, int limitCount) {

        LocalDateTime expirationDateTime = LocalDateTime.now().plusDays(Long.parseLong(expirationAt));
        Coupon coupon = Coupon.create(
                name,
                discountRate,
                maxDiscountAmount,
                expirationDateTime,
                limitCount
        );

        Coupon saved = couponRepository.save(coupon);

        couponInventoryReader.initialize(saved.getId(), limitCount, expirationDateTime);

        return saved;
    }

    public Coupon getCouponOrThrow(Long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다. couponId=" + couponId));
    }

    public Long getAvailableCouponId(Long userId) {
        if (userId == null) {
            return null;
        }
        return userCouponRepository.findAllByUserId(userId)
                .stream()
                .findFirst()
                .map(UserCoupon::getCouponId)
                .orElse(null);
    }

    @Transactional
    public void issue(Long couponId, Long userId) {
        couponInventoryReader.issue(couponId, userId);
        try {
            UserCoupon userCoupon = UserCoupon.issue(couponId, userId);
            userCouponRepository.save(userCoupon);
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

    public int calculateDiscountAmount(Long couponId, int totalAmount) {
        Coupon coupon = getCouponOrThrow(couponId);
        return coupon.calculateDiscountAmount(totalAmount);
    }

    public void updateLimitCount(Long id, int remaining) {
        Coupon coupon = getCouponOrThrow(id);
        coupon.updateLimitCount(remaining);
        couponRepository.save(coupon);
    }
}