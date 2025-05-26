package kr.hhplus.be.server.domain.product.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockEventPublisher {
    private final ApplicationEventPublisher eventPublisher;

    public void publishStockDecreased(StockDecreaseEvent event) {
        eventPublisher.publishEvent(event);
    }
}
