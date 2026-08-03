package kr.hhplus.be.server.infrastructure.payment;

import kr.hhplus.be.server.application.product.PopularProductService;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.order.OrderService;
import kr.hhplus.be.server.domain.payment.event.PaymentCompletedEvent;
import kr.hhplus.be.server.domain.payment.event.PaymentEventPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class PaymentCompletedConsumer {
    private final PopularProductService popularProductService;
    private final OrderService orderService;
    private final PaymentEventPort paymentEventPort;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String PROCESSED_KEY_PREFIX = "payment:completed:processed:";
    private static final long PROCESSED_KEY_TTL_HOURS = 24;

    @KafkaListener(topics = "${topic.payment-completed}", groupId = "payment-service")
    public void handlePaymentCompleted(PaymentCompletedEvent event, Acknowledgment acknowledgment) {
        try {
            // Kafka at-least-once 재전달로 같은 이벤트가 중복 소비될 수 있으므로,
            // paymentId 기준 멱등 키(SETNX)로 이미 처리한 메시지는 통계 반영 없이 스킵한다.
            String processedKey = PROCESSED_KEY_PREFIX + event.getPayment().getId();
            Boolean firstProcessing = redisTemplate.opsForValue()
                    .setIfAbsent(processedKey, "1", PROCESSED_KEY_TTL_HOURS, TimeUnit.HOURS);

            if (Boolean.TRUE.equals(firstProcessing)) {
                List<OrderItem> items = orderService.getOrderItems(event.getOrder().getId());
                for (OrderItem item : items) {
                    popularProductService.incrementProductSales(item.getProductId(), item.getQuantity());
                }
                paymentEventPort.send(event);
            }

            acknowledgment.acknowledge();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
