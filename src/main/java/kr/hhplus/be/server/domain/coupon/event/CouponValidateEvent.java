package kr.hhplus.be.server.domain.coupon.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class CouponValidateEvent {
    private final Long userId;
    private final Long orderId;
    private final Long couponId;

    public CouponValidateEvent(Long userId, Long orderId, Long couponId) {
        this.userId = userId;
        this.orderId = orderId;
        this.couponId = couponId;
    }
}