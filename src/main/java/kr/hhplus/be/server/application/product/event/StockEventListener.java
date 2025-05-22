package kr.hhplus.be.server.application.product.event;

import kr.hhplus.be.server.application.product.PopularProductService;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.order.OrderService;
import kr.hhplus.be.server.domain.product.ProductService;
import kr.hhplus.be.server.domain.product.event.StockDecreaseEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StockEventListener {
    private final ProductService productService;
    private final PopularProductService popularProductService;
    private final OrderService orderService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleStockDecrease(StockDecreaseEvent event) {
        List<OrderItem> items = orderService.getOrderItems(event.getOrderId());
        for (OrderItem item : items) {
            productService.decreaseStock(item.getProductId(), item.getQuantity());
            popularProductService.incrementProductSales(item.getProductId(), item.getQuantity());
        }
    }
}
