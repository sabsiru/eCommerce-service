package kr.hhplus.be.server.interfaces.coupon;

import kr.hhplus.be.server.domain.coupon.Coupon;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CouponResponse {
    private Long id;
    private String name;
    private int discountRate;
    private int maxDiscountAmount;
    private String status;
    private LocalDateTime expirationAt;
    private LocalDateTime createdAt;
    private int limitCount;
    private int issuedCount;
    private boolean expired;

    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getName(),
                coupon.getDiscountRate(),
                coupon.getMaxDiscountAmount(),
                coupon.getStatus().name(),
                coupon.getExpirationAt(),
                coupon.getCreatedAt(),
                coupon.getLimitCount(),
                coupon.getIssuedCount(),
                coupon.isExpired()
        );
    }
}
