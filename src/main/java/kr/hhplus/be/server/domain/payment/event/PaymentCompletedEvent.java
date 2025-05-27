package kr.hhplus.be.server.domain.payment.event;

import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.payment.Payment;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class PaymentCompletedEvent {
    private final Payment payment;
    private final Order order;
    private final List<OrderItem> orderItems;

    public PaymentCompletedEvent(Payment payment, Order order, List<OrderItem> orderItems) {
        this.payment = payment;
        this.order = order;
        this.orderItems = orderItems;
    }
}