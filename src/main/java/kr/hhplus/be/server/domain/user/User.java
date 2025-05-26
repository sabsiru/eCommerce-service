package kr.hhplus.be.server.domain.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "user")
public class User {

    private static final long MAX_TOTAL_CHARGE = 10_000_000L;
    private static final long MAX_SINGLE_CHARGE = 1_000_000L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private int point;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Builder
    public User(Long id, String name, int point, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.point = point;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static User create(String name, int initialPoint) {
        return User.builder()
                .name(name)
                .point(initialPoint)
                .build();
    }

    public void charge(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        }
        if (amount > MAX_SINGLE_CHARGE) {
            throw new IllegalArgumentException("1회 충전 금액은 `1,000,000원` 입니다.");
        }
        if (this.point + amount > MAX_TOTAL_CHARGE) {
            throw new IllegalArgumentException("최대 충전 한도는 `10,000,000원` 입니다.");
        }
        this.point += amount;
    }

    public void use(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("사용 금액은 0보다 커야 합니다.");
        }
        if (this.point < amount) {
            throw new IllegalStateException("포인트가 부족합니다.");
        }
        this.point -= amount;
    }

    public void refund(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("환불 금액은 0보다 커야 합니다.");
        }
        this.point += amount;
    }
}