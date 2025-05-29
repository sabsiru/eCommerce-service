package kr.hhplus.be.server.infrastructure.coupon;


import kr.hhplus.be.server.domain.coupon.event.CouponIssuedMessage;
import kr.hhplus.be.server.domain.coupon.event.CouponIssuedMessageSender;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CouponIssuedMessageHandler implements CouponIssuedMessageSender {

    private final KafkaTemplate<String, CouponIssuedMessage> kafkaTemplate;

    @Value("${topic.coupon-issued}")
    private String topic;

    @Override
    public void send(CouponIssuedMessage message) {
        kafkaTemplate.send(
                topic,
                message.getCouponId().toString(),
                message
        );
    }
}
