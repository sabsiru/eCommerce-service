package kr.hhplus.be.server.domain.order;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private Order order;

    private Long productId;

    private int quantity;

    private int orderPrice;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Builder
    private OrderItem(Long id, Order order, Long productId, int quantity, int orderPrice, LocalDateTime createdAt) {
        this.id = id;
        this.order = order;
        this.productId = productId;
        this.quantity = quantity;
        this.orderPrice = orderPrice;
        this.createdAt = createdAt;
    }

    public static OrderItem create(Order order, Long productId, int quantity, int orderPrice) {
        if (order == null) throw new IllegalArgumentException("주문 정보가 잘 못 입력 되었습니다.");
        if (productId == null) throw new IllegalArgumentException("상품 정보가 잘 못 입력 되었습니다.");
        if (quantity <= 0) throw new IllegalArgumentException("수량은 0보다 커야 합니다.");
        if (orderPrice <= 0) throw new IllegalArgumentException("주문 가격은 0보다 커야 합니다.");

        return OrderItem.builder()
                .order(order)
                .productId(productId)
                .quantity(quantity)
                .orderPrice(orderPrice)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public int totalPrice() {
        return quantity * orderPrice;
    }
}
