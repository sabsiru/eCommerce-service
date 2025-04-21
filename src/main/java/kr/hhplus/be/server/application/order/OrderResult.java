package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.domain.order.OrderStatus;
import lombok.Getter;
import lombok.AllArgsConstructor;

import java.util.List;

public final class OrderResult {
    private OrderResult() {}

    @Getter
    @AllArgsConstructor
    public static class Create {
        private final Long orderId;
        private final Long userId;
        private final List<Item> items;
        private final int totalPrice;
        private final OrderStatus status;
    }

    @Getter
    @AllArgsConstructor
    public static class Item {
        private final Long productId;
        private final int quantity;
        private final int itemPrice;
    }
}