package kr.hhplus.be.server.domain.order;

import java.time.LocalDateTime;

public record OrderItem(
        Long id,         // 주문 항목 ID
        Long orderId,    // 주문 ID
        Long productId,  // 상품 ID
        int quantity,    // 구매 수량
        int orderPrice,  // 주문 가격
        LocalDateTime createdAt  // 생성 시각
) {

    public static OrderItem create(Long orderId, Long productId, int quantity, int orderPrice) {
        if (orderId == null) {
            throw new IllegalArgumentException("주문 정보가 잘 못 입력 되었습니다.");
        }
        if (productId == null) {
            throw new IllegalArgumentException("상품 정보가 잘 못 입력 되었습니다.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("수량은 0보다 커야 합니다.");
        }
        if (orderPrice <= 0) {
            throw new IllegalArgumentException("주문 가격은 0보다 커야 합니다.");
        }
        return new OrderItem(null, orderId, productId, quantity, orderPrice, LocalDateTime.now());
    }

    public int totalPrice() {
        return quantity * orderPrice;
    }
}