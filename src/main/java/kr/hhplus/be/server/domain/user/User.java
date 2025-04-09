package kr.hhplus.be.server.domain.user;

import java.time.LocalDateTime;

public record User(
        long id,
        String name,
        long point,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    private static final long MAX_TOTAL_CHARGE = 10_000_000L; // 최대 충전한도 1000만
    private static final long MAX_SINGLE_CHARGE = 1_000_000L; // 1회 최대 충전한도 100만

    // 포인트 조회
    public long getPoint() {
        return point;
    }

    // 포인트 충전
    public User chargePoint(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        }
        if (amount > MAX_SINGLE_CHARGE) {
            throw new IllegalArgumentException("1회 충전 금액은 `1,000,000원` 입니다.");
        }
        if (this.point + amount > MAX_TOTAL_CHARGE) {
            throw new IllegalArgumentException("최대 충전 한도는 `10,000,000원` 입니다.");
        }
        return new User(id, name, this.point + amount, createdAt, LocalDateTime.now());
    }

    // 포인트 사용
    public User usePoint(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("사용 금액은 0보다 커야 합니다.");
        }
        if (this.point < amount) {
            throw new IllegalArgumentException("포인트가 부족합니다.");
        }
        return new User(id, name, this.point - amount, createdAt, LocalDateTime.now());
    }

    // 포인트 환불
    public User refundPoint(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("환불 금액은 0보다 커야 합니다.");
        }
        return new User(id, name, this.point + amount, createdAt, LocalDateTime.now());
    }
}