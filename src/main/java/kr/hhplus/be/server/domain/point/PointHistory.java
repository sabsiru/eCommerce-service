package kr.hhplus.be.server.domain.point;

import java.time.LocalDateTime;

public record PointHistory(
        Long id,
        long userId,
        long amount,
        PointHistoryType type,
        LocalDateTime createdAt,
        Long relatedOrderId
) {
    public static PointHistory charge(long userId, long amount) {
        return new PointHistory(null, userId, amount, PointHistoryType.CHARGE, LocalDateTime.now(), null);
    }

    public static PointHistory use(long userId, long amount) {
        return new PointHistory(null, userId, amount, PointHistoryType.USE, LocalDateTime.now(), null);
    }

    public static PointHistory refund(long userId, long amount, long relatedOrderId) {
        return new PointHistory(null, userId, amount, PointHistoryType.REFUND, LocalDateTime.now(), relatedOrderId);
    }
}
