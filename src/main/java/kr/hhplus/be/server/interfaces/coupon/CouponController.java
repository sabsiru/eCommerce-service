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

    @PostMapping("/{userId}/issue")
    @ResponseStatus(HttpStatus.CREATED)
    public void issueCoupon(@PathVariable Long userId,
                                  @RequestParam Long couponId) {
        couponFacade.issue(userId, couponId);
    }

    @PostMapping("/{userId}/issue-async")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void issueCouponAsync(@PathVariable Long userId,
                                 @RequestParam Long couponId) {
        couponFacade.issueAsync(couponId, userId);
        
    }

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public void createCoupon(@RequestParam String name,
                               @RequestParam int discountRate,
                               @RequestParam int maxDiscountAmount,
                               @RequestParam String expirationAt,
                               @RequestParam int limitCount) {
        couponFacade.create(name, discountRate, maxDiscountAmount, expirationAt, limitCount);
    }

    @GetMapping
    public Coupon getCoupon(@RequestParam Long couponId) {
        return couponFacade.getCouponOrThrow(couponId);
    }
}
