package kr.hhplus.be.server.domain.payment.event;

import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.payment.Payment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class PaymentCompletedEvent {
    private final Payment payment;
    private final Order order;
    private final List<OrderItem> orderItems;
}