package kr.hhplus.be.server.domain.payment;

import java.time.LocalDateTime;

public record Payment(
        Long id,
        long orderId,
        int amount,
        PaymentStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long couponId
) {

    public static Payment initiate(long orderId, int amount) {
        LocalDateTime now = LocalDateTime.now();
        return new Payment(null, orderId, amount, PaymentStatus.PENDING, now, now, null);
    }

    // 쿠폰 사용한 결제 초기화: couponId를 전달받음
    public static Payment initiate(long orderId, int amount, Long couponId) {
        LocalDateTime now = LocalDateTime.now();
        return new Payment(null, orderId, amount, PaymentStatus.PENDING, now, now, couponId);
    }

    public Payment complete() {
        if (this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException("결제가 이미 완료되었습니다.");
        }
        return new Payment(this.id, this.orderId, this.amount, PaymentStatus.COMPLETED, this.createdAt, LocalDateTime.now(), this.couponId);
    }

    public Payment refund() {
        if (this.status != PaymentStatus.COMPLETED) {
            throw new IllegalStateException("결제가 진행 중인 상태가 아닙니다.");
        }
        return new Payment(this.id, this.orderId, this.amount, PaymentStatus.REFUND, this.createdAt, LocalDateTime.now(), this.couponId);
    }
}