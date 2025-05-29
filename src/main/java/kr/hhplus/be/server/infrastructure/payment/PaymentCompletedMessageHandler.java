package kr.hhplus.be.server.infrastructure.payment;

import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.order.OrderService;
import kr.hhplus.be.server.domain.payment.Payment;
import kr.hhplus.be.server.domain.payment.event.PaymentCompletedEvent;
import kr.hhplus.be.server.domain.payment.event.PaymentCompletedMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PaymentCompletedMessageHandler implements PaymentCompletedMessage {
    private final KafkaTemplate<String, PaymentCompletedEvent> kafkaTemplate;
    private final OrderService orderService;

    @Value("${topic.payment-completed}")
    private String topic;

    @Override
    public void send(Payment payment, Order order) {
        System.out.println("카프카 메시지 발송 시도: 주문ID=" + order.getId() + ", 토픽=" + topic);
        List<OrderItem> items = orderService.getOrderItems(order.getId());
        PaymentCompletedEvent event = new PaymentCompletedEvent(payment, order, items);
        kafkaTemplate.send(
                topic,
                payment.getId().toString(),
                event
        );
        System.out.println("카프카 메시지 발송 완료");
    }
}