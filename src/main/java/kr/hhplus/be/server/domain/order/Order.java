package kr.hhplus.be.server.domain.order;

import jakarta.persistence.*;
import kr.hhplus.be.server.application.order.OrderItemCommand;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "orders")  // "order"는 예약어이므로 테이블명 변경
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private int totalAmount;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
    @Transient
    private List<OrderItem> items = new ArrayList<>();

    @Builder
    public Order(Long id, Long userId, List<OrderItem> items, OrderStatus status, LocalDateTime createdAt, LocalDateTime updatedAt,int totalAmount) {
        this.id = id;
        this.userId = userId;
        this.items = items;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.totalAmount = totalAmount; // 실제 금액 계산은 create()에서 처리
    }

    public static Order create(Long userId, List<OrderItemCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            throw new IllegalArgumentException("주문 항목이 비어 있습니다.");
        }

        Order order = new Order(userId); // 생성자 내부에서 상태 초기화 및 시간 설정
        order.userId = userId;
        order.status = OrderStatus.PENDING;
        for (OrderItemCommand cmd : commands) {
            OrderItem item = OrderItem.create(order, cmd.getProductId(), cmd.getQuantity(), cmd.getOrderPrice());
            item.setOrder(order);
            order.addItem(item);
        }

        order.totalAmount = order.calculateTotalAmount(); // 총합 계산
        return order;
    }

    public Order(Long userId) {
        this.userId = userId;
        this.status = OrderStatus.PENDING;
    }

    public void addItem(OrderItem item) {
        item.setOrder(this);
        this.items.add(item);
    }

    // 결제 처리
    public void pay() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("결제는 PENDING 상태의 주문에만 가능합니다.");
        }
        this.status = OrderStatus.PAID;
    }

    // 주문 취소
    public void cancel() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("이미 결제 완료된 주문은 취소할 수 없습니다.");
        }
        this.status = OrderStatus.CANCEL;
    }

    // 총액 재계산
    public int calculateTotalAmount() {
        return this.items.stream()
                .mapToInt(OrderItem::totalPrice)
                .sum();
    }

    // 주문 항목 갱신
    public void updateItems(List<OrderItem> newItems) {
        this.items.clear();
        this.items.addAll(newItems);
        for (OrderItem item : newItems) {
            item.setOrder(this);
        }
        this.totalAmount = calculateTotalAmount();
    }
}