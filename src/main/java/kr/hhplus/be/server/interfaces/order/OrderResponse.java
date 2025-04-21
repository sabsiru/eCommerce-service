package kr.hhplus.be.server.interfaces.order;

import kr.hhplus.be.server.application.order.OrderResult;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * HTTP Response 반환용 DTO — Jackson 직렬화 위해 no-args 필요
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private Long userId;
    private List<Item> items;
    private int totalAmount;
    private OrderStatus status;

    public static OrderResponse from(Order order) {
        List<Item> dtoItems = order.getItems().stream()
                .map(i -> new Item(i.getProductId(), i.getQuantity(), i.getOrderPrice()))
                .collect(Collectors.toList());

        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                dtoItems,
                order.getTotalAmount(),
                order.getStatus()
        );
    }

    public static OrderResponse from(OrderResult.Create result) {
        List<Item> itemList = result.getItems().stream()
                .map(i -> new Item(i.getProductId(), i.getQuantity(), i.getItemPrice()))
                .collect(Collectors.toList());

        return new OrderResponse(
                result.getOrderId(),
                result.getUserId(),
                itemList,
                result.getTotalPrice(),
                result.getStatus()
        );
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private Long productId;
        private int quantity;
        private int itemPrice;
    }
}