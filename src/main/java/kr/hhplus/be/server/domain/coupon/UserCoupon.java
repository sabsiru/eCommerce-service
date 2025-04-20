package kr.hhplus.be.server.domain.coupon;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private Long couponId;

    @Enumerated(EnumType.STRING)
    private UserCouponStatus status;

    private LocalDateTime issuedAt;

    private LocalDateTime usedAt;

    @Builder
    public UserCoupon(Long id, Long userId, Long couponId, UserCouponStatus status, LocalDateTime issuedAt, LocalDateTime usedAt) {
        this.id = id;
        this.userId = userId;
        this.couponId = couponId;
        this.status = status;
        this.issuedAt = issuedAt;
        this.usedAt = usedAt;
    }

    public static UserCoupon issue(Long userId, Long couponId) {
        return UserCoupon.builder()
                .userId(userId)
                .couponId(couponId)
                .status(UserCouponStatus.ISSUED)
                .issuedAt(LocalDateTime.now())
                .usedAt(null)
                .build();
    }

    public void use() {
        if (this.status != UserCouponStatus.ISSUED) {
            throw new IllegalStateException("사용 할 수 없는 쿠폰 입니다.");
        }
        this.status = UserCouponStatus.USED;
        this.usedAt = LocalDateTime.now();
    }

    public void refund() {
        if (this.status != UserCouponStatus.USED) {
            throw new IllegalStateException("환불 가능한 쿠폰이 아닙니다.");
        }
        this.status = UserCouponStatus.ISSUED;
        this.usedAt = null;
    }
}