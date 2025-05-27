package kr.hhplus.be.server.domain.coupon.event;

import kr.hhplus.be.server.domain.coupon.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class CouponEventListener {
    private final CouponService couponService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleCouponUse(CouponValidateEvent event) {
        if (event.getCouponId() != null) {
            couponService.use(event.getCouponId());
        }
    }
}