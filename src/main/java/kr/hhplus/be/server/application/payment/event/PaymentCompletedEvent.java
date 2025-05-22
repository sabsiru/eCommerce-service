package kr.hhplus.be.server.application.payment.event;

import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.payment.Payment;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class PaymentCompletedEvent extends ApplicationEvent {
    private final Payment payment;
    private final Order order;
    private final List<OrderItem> orderItems;
    private final String source;

    public PaymentCompletedEvent(Payment payment, Order order, List<OrderItem> orderItems, String source) {
        super(payment);
        this.payment = payment;
        this.order = order;
        this.orderItems = orderItems;
        this.source = source;
    }
}