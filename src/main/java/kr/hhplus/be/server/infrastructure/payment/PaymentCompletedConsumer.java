package kr.hhplus.be.server.infrastructure.payment;

import kr.hhplus.be.server.application.product.PopularProductService;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.order.OrderService;
import kr.hhplus.be.server.domain.payment.event.PaymentCompletedEvent;
import kr.hhplus.be.server.domain.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PaymentCompletedConsumer {
    private final ProductService productService;
    private final PopularProductService popularProductService;
    private final OrderService orderService;

    @KafkaListener(topics = "${topic.payment-completed}", groupId = "payment-service")
    public void handlePaymentCompleted(PaymentCompletedEvent event, Acknowledgment acknowledgment) {
        try {
            List<OrderItem> items = orderService.getOrderItems(event.getOrder().getId());
            for (OrderItem item : items) {
                productService.decreaseStock(item.getProductId(), item.getQuantity());
                popularProductService.incrementProductSales(item.getProductId(), item.getQuantity());
            }
            acknowledgment.acknowledge();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
