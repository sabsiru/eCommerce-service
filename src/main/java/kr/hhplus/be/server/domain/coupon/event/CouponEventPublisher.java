package kr.hhplus.be.server.domain.coupon.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CouponEventPublisher {
    private final ApplicationEventPublisher eventPublisher;

    public void publishCouponValidate(CouponValidateEvent event) {
        eventPublisher.publishEvent(event);
    }
}
