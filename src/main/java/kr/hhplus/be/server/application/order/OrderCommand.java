package kr.hhplus.be.server.application.order;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

public final class OrderCommand {
    private OrderCommand() {}

    @Getter
    @AllArgsConstructor
    public static class Create {
        private final Long userId;
        private final List<Item> items;
    }

    @Getter
    @AllArgsConstructor
    public static class Item {
        private final Long productId;
        private final int quantity;
        private final int itemPrice;
    }
}