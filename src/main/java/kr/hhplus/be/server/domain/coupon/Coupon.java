package kr.hhplus.be.server.domain.coupon;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private int discountRate;

    private int maxDiscountAmount;

    @Enumerated(EnumType.STRING)
    private CouponStatus status;

    private LocalDateTime expirationAt;

    private LocalDateTime createdAt;

    private int limitCount;

    private int issuedCount;

    @Builder
    public Coupon(Long id, String name, int discountRate, int maxDiscountAmount, CouponStatus status,
                  LocalDateTime expirationAt, LocalDateTime createdAt, int limitCount, int issuedCount) {
        validateDiscountRate(discountRate);
        this.id = id;
        this.name = name;
        this.discountRate = discountRate;
        this.maxDiscountAmount = maxDiscountAmount;
        this.status = status;
        this.expirationAt = expirationAt;
        this.createdAt = createdAt;
        this.limitCount = limitCount;
        this.issuedCount = issuedCount;
    }

    public static Coupon create(String name, int discountRate, int maxDiscountAmount,
                                LocalDateTime expirationAt, int limitCount) {
        validateDiscountRate(discountRate);
        return Coupon.builder()
                .name(name)
                .discountRate(discountRate)
                .maxDiscountAmount(maxDiscountAmount)
                .status(CouponStatus.ACTIVE)
                .expirationAt(expirationAt)
                .createdAt(LocalDateTime.now())
                .limitCount(limitCount)
                .issuedCount(0)
                .build();
    }

    public void validateUsable() {
        if (issuedCount >= limitCount) {
            throw new IllegalStateException("쿠폰 발급 수량이 모두 소진되었습니다.");
        }
        if (isExpired()) {
            throw new IllegalArgumentException("만료된 쿠폰입니다.");
        }
        if (status != CouponStatus.ACTIVE) {
            throw new IllegalArgumentException("사용 할 수 없는 쿠폰 입니다.");
        }
    }

    public boolean isExpired() {
        return expirationAt.isBefore(LocalDateTime.now());
    }

    private static void validateDiscountRate(int rate) {
        if (rate <= 0 || rate > 50) {
            throw new IllegalArgumentException("할인율은 1% 이상 50% 이하여야 합니다.");
        }
    }

    public Coupon increaseIssuedCount() {
        validateUsable();
        int updatedIssuedCount = this.issuedCount + 1;

        if (updatedIssuedCount > this.limitCount) {
            throw new IllegalStateException("쿠폰 발급 수량이 모두 소진되었습니다.");
        }

        CouponStatus updatedStatus = (updatedIssuedCount == this.limitCount)
                ? CouponStatus.EXPIRED
                : this.status;

        return new Coupon(
                this.id,
                this.name,
                this.discountRate,
                this.maxDiscountAmount,
                updatedStatus,
                this.expirationAt,
                this.createdAt,
                this.limitCount,
                updatedIssuedCount
        );
    }

    public int calculateDiscountAmount(int orderAmount) {
        int discount = (orderAmount * discountRate) / 100;
        return Math.min(discount, maxDiscountAmount);
    }

    public Coupon expire() {
        return new Coupon(
                this.id,
                this.name,
                this.discountRate,
                this.maxDiscountAmount,
                CouponStatus.EXPIRED,
                this.expirationAt,
                this.createdAt,
                this.limitCount,
                this.issuedCount
        );
    }
}
