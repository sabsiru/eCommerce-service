package kr.hhplus.be.server.application.order;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class OrderItemRequest {
    private Long productId;
    private int quantity;
    private int orderPrice;

    public OrderItemRequest(Long productId, int quantity, int orderPrice) {
        this.productId = productId;
        this.quantity = quantity;
        this.orderPrice = orderPrice;
    }
}

