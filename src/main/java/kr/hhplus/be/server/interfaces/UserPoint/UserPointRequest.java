package kr.hhplus.be.server.interfaces.UserPoint;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserPointRequest {
    @NotNull
    private Long userId;

    // chargeAmount 범위(0보다 커야 함, 1회/총 한도)는 기존 도메인 로직(UserPointFacade)이
    // 구체적인 한국어 메시지로 이미 검증하고 있어 여기서 중복 제약을 걸지 않는다.
    private int chargeAmount;
}
