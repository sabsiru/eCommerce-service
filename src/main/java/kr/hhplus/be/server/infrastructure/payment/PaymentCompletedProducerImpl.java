package kr.hhplus.be.server.infrastructure.payment;

import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.order.OrderService;
import kr.hhplus.be.server.domain.payment.Payment;
import kr.hhplus.be.server.domain.payment.event.PaymentCompletedEvent;
import kr.hhplus.be.server.domain.payment.event.PaymentCompletedProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PaymentCompletedProducerImpl implements PaymentCompletedProducer {
    private final KafkaTemplate<String, PaymentCompletedEvent> kafkaTemplate;
    private final OrderService orderService;

    @Value("${topic.payment-completed}")
    private String topic;

    @Override
    public void send(Payment payment, Order order) {
        List<OrderItem> items = orderService.getOrderItems(order.getId());
        PaymentCompletedEvent event = new PaymentCompletedEvent(payment, order, items);
        kafkaTemplate.send(
                topic,
                payment.getId().toString(),
                event
        );
    }
}