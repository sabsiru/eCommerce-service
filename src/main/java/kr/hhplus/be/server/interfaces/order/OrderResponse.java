package kr.hhplus.be.server.interfaces.order;

import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderStatus;

import java.util.List;

public record OrderResponse(
        Long id,
        Long userId,
        List<OrderItemResponse> items,
        int totalAmount,
        OrderStatus status
) {
    public static OrderResponse from(Order order) {
        List<OrderItemResponse> itemResponses = order.items().stream()
                .map(OrderItemResponse::from)
                .toList();
        return new OrderResponse(
                order.id(),
                order.userId(),
                itemResponses,
                order.totalAmount(),
                order.status()
        );
    }
}
