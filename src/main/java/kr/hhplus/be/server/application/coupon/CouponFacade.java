package kr.hhplus.be.server.application.coupon;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.CouponService;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.domain.coupon.event.CouponIssuedMessage;
import kr.hhplus.be.server.domain.coupon.event.CouponIssuedMessageSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CouponFacade {

    private final CouponService couponService;
    private final CouponIssuedMessageSender couponIssuedMessageSender;

    public UserCoupon issue(Long userId, Long couponId) {

        return couponService.issue(userId, couponId);
    }

    public void issueAsync(Long userId, Long couponId) {
        CouponIssuedMessage couponIssuedMessage = new CouponIssuedMessage(userId, couponId);
        couponIssuedMessageSender.send(couponIssuedMessage);
    }

    @Transactional
    public Coupon getCouponOrThrow(Long couponId) {

        return couponService.getCouponOrThrow(couponId);
    }
}