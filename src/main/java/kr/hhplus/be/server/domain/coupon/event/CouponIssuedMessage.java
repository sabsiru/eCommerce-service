package kr.hhplus.be.server.domain.coupon.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(force = true)
@AllArgsConstructor
@Getter
public class CouponIssuedMessage {
    private Long couponId;
    private Long userId;
}