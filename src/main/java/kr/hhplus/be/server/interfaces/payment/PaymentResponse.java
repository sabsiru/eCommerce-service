package kr.hhplus.be.server.interfaces.payment;

import kr.hhplus.be.server.domain.payment.Payment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private Long orderId;
    private int amount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long couponId;

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getStatus().name(),
                payment.getCreatedAt(),
                payment.getUpdatedAt(),
                payment.getCouponId()
        );
    }
}