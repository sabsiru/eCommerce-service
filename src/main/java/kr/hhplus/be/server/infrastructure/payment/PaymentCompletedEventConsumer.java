package kr.hhplus.be.server.infrastructure.payment;

import kr.hhplus.be.server.application.product.PopularProductService;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.order.OrderService;
import kr.hhplus.be.server.domain.payment.event.PaymentCompletedEvent;
import kr.hhplus.be.server.domain.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PaymentCompletedEventConsumer {
    private final ProductService productService;
    private final PopularProductService popularProductService;
    private final OrderService orderService;

    @KafkaListener(topics = "${topic.payment-completed}", groupId = "payment-service")
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        System.out.println("메시지 수신: 주문ID=" + event.getOrder().getId());
        List<OrderItem> items = orderService.getOrderItems(event.getOrder().getId());
        for (OrderItem item : items) {
            productService.decreaseStock(item.getProductId(), item.getQuantity());
            popularProductService.incrementProductSales(item.getProductId(), item.getQuantity());
        }
        System.out.println("메시지 처리 완료: 주문ID=" + event.getOrder().getId());
    }
}
