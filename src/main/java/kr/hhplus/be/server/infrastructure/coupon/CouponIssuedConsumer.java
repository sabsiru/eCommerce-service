package kr.hhplus.be.server.infrastructure.coupon;

import kr.hhplus.be.server.domain.coupon.CouponService;
import kr.hhplus.be.server.domain.coupon.event.CouponIssuedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class CouponIssuedConsumer {

    private final CouponService couponService;

    @KafkaListener(
            topics = "${topic.coupon-issued}",
            groupId = "coupon-issuer"
    )
    public void consume(CouponIssuedMessage message, Acknowledgment ack) {
        try {
            couponService.issue(
                    message.getCouponId(),
                    message.getUserId()
            );
            ack.acknowledge();
        } catch (IllegalStateException e) {
            String msg = e.getMessage();

            if ("이미 발급받은 사용자입니다.".equals(msg)) {
                log.warn("중복 발급 시도: couponId={}, userId={}",
                        message.getCouponId(), message.getUserId());
                ack.acknowledge();
                return;
            }

            if ("재고가 소진되었습니다.".equals(msg)) {
                log.warn("재고 소진 상태: couponId={}, userId={}",
                        message.getCouponId(), message.getUserId());
                ack.acknowledge();
                return;
            }

            if ("발급이 종료된 쿠폰입니다.".equals(msg)) {
                log.warn("발급 종료된 쿠폰 접근: couponId={}, userId={}",
                        message.getCouponId(), message.getUserId());
                ack.acknowledge();
                return;
            }

        } catch (Exception e) {
            throw e;
        }
    }
}
