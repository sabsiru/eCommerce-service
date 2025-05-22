package kr.hhplus.be.server.application.payment.event;

import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.order.OrderService;
import kr.hhplus.be.server.domain.payment.Payment;
import kr.hhplus.be.server.domain.order.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {
    private final ApplicationEventPublisher eventPublisher;
    private final OrderService orderService;

    public void publishPaymentCompleted(Payment payment, Order order) {
        List<OrderItem> orderItems = orderService.getOrderItems(order.getId());

        PaymentCompletedEvent event = new PaymentCompletedEvent(
                payment,
                order,
                orderItems,
                "payment_system"
        );

        eventPublisher.publishEvent(event);
    }
}
