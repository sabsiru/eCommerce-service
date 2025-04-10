package kr.hhplus.be.server.domain.coupon;

import java.time.LocalDateTime;

public record Coupon(
        Long id,
        String name,
        int discountRate,         // 할인율 (%)
        int maxDiscountAmount,    // 최대 할인 금액
        CouponStatus status,      // 상태: 활성화, 만료
        LocalDateTime expirationAt, // 만료 일자
        LocalDateTime createdAt,
        int limitCount,
        int issuedCount // 현재 발급된 수량
) {
    public Coupon {
        validateDiscountRate(discountRate);
    }

    public static Coupon create(
            Long id,
            String name,
            int discountRate,
            int maxDiscountAmount,
            LocalDateTime expirationAt,
            int limitCount
    ) {
        validateDiscountRate(discountRate);
        return new Coupon(
                id,
                name,
                discountRate,
                maxDiscountAmount,
                CouponStatus.ACTIVE,
                expirationAt,
                LocalDateTime.now(),
                limitCount,
                0
        );
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

        // 한도에 도달하면 상태를 EXPIRED로 변경
        CouponStatus updatedStatus = (updatedIssuedCount == this.limitCount)
                ? CouponStatus.EXPIRED
                : this.status;

        return new Coupon(
                id,
                name,
                discountRate,
                maxDiscountAmount,
                updatedStatus,
                expirationAt,
                createdAt,
                limitCount,
                updatedIssuedCount
        );
    }

    public int calculateDiscountAmount(int orderAmount) {
        int discount = (orderAmount * discountRate) / 100;
        return Math.min(discount, maxDiscountAmount);
    }


    public Coupon expire() {
        return new Coupon(
                id,
                name,
                discountRate,
                maxDiscountAmount,
                CouponStatus.EXPIRED,
                expirationAt,
                createdAt,
                limitCount,
                issuedCount
        );
    }

}