package kr.hhplus.be.server.infrastructure.coupon;

import kr.hhplus.be.server.domain.coupon.CouponService;
import kr.hhplus.be.server.domain.coupon.event.CouponIssuedMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CouponIssuedConsumer {

    private final CouponService couponService;

    @KafkaListener(
            topics = "${topic.coupon-issued}",
            groupId = "coupon-issuer"
    )
    public void consume(CouponIssuedMessage message) {
        couponService.issue(
                message.getCouponId(),
                message.getUserId()
        );
    }
}
