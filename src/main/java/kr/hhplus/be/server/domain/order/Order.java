package kr.hhplus.be.server.domain.order;

import jakarta.persistence.*;
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

    /** 최소 필드만 초기화 **/
    public Order(Long userId) {
        this.userId = userId;
        this.status = OrderStatus.PENDING;
        this.totalAmount = 0;
    }

    /**
     * 주문 항목 추가 — OrderItem 생성, 연관관계 설정, 금액 합산
     */
    public void addLine(Long productId, int quantity, int orderPrice) {
        if (productId == null)      throw new IllegalArgumentException("상품 정보가 잘못 입력되었습니다.");
        if (quantity <= 0)          throw new IllegalArgumentException("수량은 0보다 커야 합니다.");
        if (orderPrice <= 0)        throw new IllegalArgumentException("주문 가격은 0보다 커야 합니다.");

        OrderItem item = new OrderItem(this, productId, quantity, orderPrice);
        this.items.add(item);
        this.totalAmount += item.totalPrice();
    }

    /** 결제 처리 **/
    public void pay() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("결제는 PENDING 상태의 주문에만 가능합니다.");
        }
        this.status = OrderStatus.PAID;
    }

    /** 주문 취소 **/
    public void cancel() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("이미 결제 완료된 주문은 취소할 수 없습니다.");
        }
        this.status = OrderStatus.CANCEL;
    }

    /** 총액 재계산 (주로 테스트나 복구용) **/
    public int calculateTotalAmount() {
        return this.items.stream()
                .mapToInt(OrderItem::totalPrice)
                .sum();
    }

    /**
     * 항목 전체 교체 (연관관계 재설정 및 금액 재계산)
     */
    public void updateItems(List<OrderItem> newItems) {
        this.items.clear();
        this.items.addAll(newItems);
        this.totalAmount = calculateTotalAmount();
    }
}