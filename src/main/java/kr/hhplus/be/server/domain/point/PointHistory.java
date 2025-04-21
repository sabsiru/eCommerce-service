package kr.hhplus.be.server.domain.point;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "point_history")
public class PointHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long userId;

    private long amount;

    @Enumerated(EnumType.STRING)
    private PointHistoryType type;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private Long orderId;

    public static PointHistory charge(long userId, long amount) {
        return new PointHistory(userId, amount, PointHistoryType.CHARGE, null);
    }

    public static PointHistory use(long userId, long amount) {
        return new PointHistory(userId, amount, PointHistoryType.USE, null);
    }

    public static PointHistory refund(long userId, long amount, long orderId) {
        return new PointHistory(userId, amount, PointHistoryType.REFUND, orderId);
    }

    @Builder(builderMethodName = "testBuilder")
    public PointHistory(
            Long id,
            long userId,
            long amount,
            PointHistoryType type,
            LocalDateTime createdAt,
            Long orderId
    ) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.createdAt = createdAt;
        this.orderId = orderId;
    }

    private PointHistory(long userId, long amount, PointHistoryType type, Long orderId) {
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.orderId = orderId;
    }
}