package kr.hhplus.be.server.interfaces.order;

import kr.hhplus.be.server.domain.order.OrderItem;

public record OrderItemResponse(
        Long productId,
        int quantity,
        int orderPrice
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(item.productId(), item.quantity(), item.orderPrice());
    }
}
