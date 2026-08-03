package kr.hhplus.be.server.infrastructure.coupon;

import kr.hhplus.be.server.domain.coupon.CouponIssuanceClosedException;
import kr.hhplus.be.server.domain.coupon.CouponService;
import kr.hhplus.be.server.domain.coupon.CouponSoldOutException;
import kr.hhplus.be.server.domain.coupon.DuplicateCouponIssueException;
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
                    message.getUserId(),
                    message.getCouponId()
            );
            ack.acknowledge();
        } catch (DuplicateCouponIssueException e) {
            log.warn("중복 발급 시도: couponId={}, userId={}",
                    message.getCouponId(), message.getUserId());
            ack.acknowledge();
        } catch (CouponSoldOutException e) {
            log.warn("재고 소진 상태: couponId={}, userId={}",
                    message.getCouponId(), message.getUserId());
            ack.acknowledge();
        } catch (CouponIssuanceClosedException e) {
            log.warn("발급 종료된 쿠폰 접근: couponId={}, userId={}",
                    message.getCouponId(), message.getUserId());
            ack.acknowledge();
        }
    }
}
