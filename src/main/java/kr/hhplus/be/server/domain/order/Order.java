package kr.hhplus.be.server.domain.order;

import jakarta.persistence.Entity;

import java.time.LocalDateTime;
import java.util.List;

public record Order(
        Long id,                // 주문 ID
        Long userId,            // 주문한 사용자 ID
        List<OrderItem> items,  // 주문 항목 목록
        int totalAmount,        // 주문 총 금액 (주문 항목 총액 합계)
        OrderStatus status,     // 주문 상태: PENDING, PAID, CANCEL, REFUND
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static Order create(Long userId, List<OrderItem> items) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("주문을 찾을 수 없습니다.");
        }
        int totalAmount = items.stream().mapToInt(OrderItem::totalPrice).sum();

        LocalDateTime now = LocalDateTime.now();
        return new Order(null, userId, items, totalAmount, OrderStatus.PENDING, now, now);
    }

    public Order pay() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("결제는 PENDING 상태의 주문에만 가능합니다.");
        }
        return new Order(this.id, this.userId, this.items, this.totalAmount, OrderStatus.PAID, this.createdAt, LocalDateTime.now());
    }

    public Order cancel() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("이미 결제 완료된 주문은 취소할 수 없습니다.");
        }
        return new Order(this.id, this.userId, this.items, this.totalAmount, OrderStatus.CANCEL, this.createdAt, LocalDateTime.now());
    }

    public int calculateTotalAmount() {
        return items.stream().mapToInt(OrderItem::totalPrice).sum();
    }

    public Order updateItems(List<OrderItem> newItems) {
        int recalculatedTotal = newItems.stream()
                .mapToInt(OrderItem::totalPrice)
                .sum();
        return new Order(
                this.id,
                this.userId,
                newItems,
                recalculatedTotal,
                this.status,
                this.createdAt,
                LocalDateTime.now() // updatedAt 갱신
        );
    }
}