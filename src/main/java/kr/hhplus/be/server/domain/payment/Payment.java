package kr.hhplus.be.server.domain.payment;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;

    private int amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private Long couponId;

    @Builder
    private Payment(Long id, Long orderId, int amount, PaymentStatus status, LocalDateTime createdAt, LocalDateTime updatedAt, Long couponId) {
        this.id = id;
        this.orderId = orderId;
        this.amount = amount;
        this.status = status != null ? status : PaymentStatus.PENDING;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.couponId = couponId;
    }

    public static Payment create(Long orderId, int amount, Long couponId) {
        return Payment.builder()
                .orderId(orderId)
                .amount(amount)
                .couponId(couponId)
                .status(PaymentStatus.COMPLETED)
                .build();
    }

    public static Payment withoutCoupon(Long orderId, int amount) {
        return Payment.builder()
                .orderId(orderId)
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .build();
    }

    public void complete() {
        if (this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException("결제가 이미 완료되었습니다.");
        }
        this.status = PaymentStatus.COMPLETED;
    }

    public void refund() {
        if (this.status != PaymentStatus.COMPLETED) {
            throw new IllegalStateException("결제가 완료되지 않은 주문입니다.");
        }
        this.status = PaymentStatus.REFUND;
    }
}
