package kr.hhplus.be.server.application.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@RequiredArgsConstructor
public class ChargePointRequest {
    private Long userId;
    private long chargeAmount;
}
