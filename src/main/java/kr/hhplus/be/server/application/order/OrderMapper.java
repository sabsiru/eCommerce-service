package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.domain.order.OrderItem;

import java.util.List;
import java.util.stream.Collectors;

public class OrderMapper {

    private OrderMapper() {} // 정적 유틸 클래스

    public static List<OrderResult.Item> toResultItems(List<OrderItem> items) {
        return items.stream()
                .map(i -> new OrderResult.Item(
                        i.getProductId(),
                        i.getQuantity(),
                        i.getOrderPrice()
                ))
                .collect(Collectors.toList());
    }
}