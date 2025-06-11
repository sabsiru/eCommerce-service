package kr.hhplus.be.server.application.coupon;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.CouponService;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.domain.coupon.event.CouponIssuedMessage;
import kr.hhplus.be.server.domain.coupon.event.CouponIssuedProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CouponFacade {

    private final CouponService couponService;
    private final CouponIssuedProducer couponIssuedProducer;

    public void issue(Long userId, Long couponId) {

         couponService.issue(userId, couponId);
    }

    public void issueAsync( Long couponId, Long userId) {
        CouponIssuedMessage couponIssuedMessage = new CouponIssuedMessage(couponId,userId);
        couponIssuedProducer.send(couponIssuedMessage);
    }

    @Transactional
    public Coupon getCouponOrThrow(Long couponId) {

        return couponService.getCouponOrThrow(couponId);
    }

    public void create(String name, int discountRate,
                       int maxDiscountAmount, String expirationAt, int limitCount) {
        couponService.create(name, discountRate, maxDiscountAmount, expirationAt, limitCount);
    }
}