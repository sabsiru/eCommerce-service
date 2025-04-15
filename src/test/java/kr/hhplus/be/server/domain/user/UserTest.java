package kr.hhplus.be.server.domain.user;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

class UserTest {

    @Test
    void 포인트_충전_테스트() {
        // Given
        User user = new User(1L, "tester", 5000, LocalDateTime.now(), LocalDateTime.now());

        // When
        user.chargePoint(1000);

        // Then
        assertEquals(6000, user.getPoint());
    }

    @Test
    void 포인트_사용_테스트() {
        // Given
        User user = new User(1L, "tester", 5000, LocalDateTime.now(), LocalDateTime.now());

        // When
        user.usePoint(2000);

        // Then
        assertEquals(3000, user.getPoint());
    }

    @Test
    void 포인트_환불_테스트() {
        // Given
        User user = new User(1L, "tester", 5000, LocalDateTime.now(), LocalDateTime.now());

        // When
        user.refundPoint(1000);

        // Then
        assertEquals(6000, user.getPoint());
    }

    @Test
    void 음수_충전_예외_테스트() {
        // Given
        User user = new User(1L, "tester", 5000, LocalDateTime.now(), LocalDateTime.now());

        // When & Then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> user.chargePoint(-100));
        assertEquals("충전 금액은 0보다 커야 합니다.", e.getMessage());
    }

    @Test
    void 최대_충전금액_초과_예외_테스트() {
        // Given
        User user = new User(1L, "tester", 9500000, LocalDateTime.now(), LocalDateTime.now());

        // When & Then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> user.chargePoint(500001));
        assertEquals("최대 충전 한도는 `10,000,000원` 입니다.", e.getMessage());
    }

    @Test
    void 충전금액_1회_한도초과_예외_테스트() {
        // Given
        User user = new User(1L, "tester", 5000, LocalDateTime.now(), LocalDateTime.now());

        // When & Then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> user.chargePoint(1000001));
        assertEquals("1회 충전 금액은 `1,000,000원` 입니다.", e.getMessage());
    }

    @Test
    void 잔고_부족_사용_예외_테스트() {
        // Given
        User user = new User(1L, "tester", 5000, LocalDateTime.now(), LocalDateTime.now());

        // When & Then
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> user.usePoint(6000));
        assertEquals("포인트가 부족합니다.", e.getMessage());
    }
}