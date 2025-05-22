package kr.hhplus.be.server.domain.product.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class StockDecreaseEvent extends ApplicationEvent {
        private final Long orderId;

    public StockDecreaseEvent(Long orderId) {
        super(orderId);
        this.orderId = orderId;
    }

}
