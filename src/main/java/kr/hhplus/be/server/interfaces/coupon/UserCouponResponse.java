package kr.hhplus.be.server.interfaces.coupon;

import kr.hhplus.be.server.domain.coupon.UserCoupon;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserCouponResponse {
    private Long id;
    private Long userId;
    private Long couponId;
    private String status;
    private LocalDateTime issuedAt;
    private LocalDateTime usedAt;

    public static UserCouponResponse from(UserCoupon userCoupon) {
        return new UserCouponResponse(
                userCoupon.getId(),
                userCoupon.getUserId(),
                userCoupon.getCouponId(),
                userCoupon.getStatus().name(),
                userCoupon.getIssuedAt(),
                userCoupon.getUsedAt()
        );
    }
}
