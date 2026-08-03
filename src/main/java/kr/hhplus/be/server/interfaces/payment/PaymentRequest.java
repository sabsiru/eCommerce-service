package kr.hhplus.be.server.interfaces.payment;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    // int(원시타입)는 null이 될 수 없어 @NotNull은 항상 통과하는 무의미한 검증이었다.
    // 실제로 막아야 할 건 0 이하의 조작된 결제 금액이므로 @Positive로 교체.
    @Positive
    private int paymentAmount;
}