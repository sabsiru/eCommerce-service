package kr.hhplus.be.server.interfaces.coupon;

import kr.hhplus.be.server.application.coupon.CouponFacade;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/coupons")
public class CouponController {

    private final CouponFacade couponFacade;

    /**
     * 사용자에게 쿠폰 발급 엔드포인트
     * 예: POST /coupons/issue?userId=100&couponId=500
     */
    @PostMapping("/{userId}issue")
    @ResponseStatus(HttpStatus.CREATED)
    public UserCoupon issueCoupon(@PathVariable Long userId,
                                  @RequestParam Long couponId) {
        return couponFacade.issueCoupon(userId, couponId);
    }

    /**
     * 쿠폰 단건 조회 엔드포인트
     * 예: GET /coupons?userId=100&couponId=500
     */
    @GetMapping
    public Coupon getCoupon(@RequestParam Long couponId) {
        return couponFacade.getCouponOrThrow(couponId);
    }
}
