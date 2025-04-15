package kr.hhplus.be.server.application.user;

import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserPointService {

    private final UserRepository userRepository;

    // 포인트 충전
    public User chargePoint(long userId, long amount) {
        User user = getUserOrThrow(userId);
        user.chargePoint(amount);

        return user;
    }

    // 포인트 사용
    public User usePoint(long userId, long amount) {
        User user = getUserOrThrow(userId);
        user.usePoint(amount);// 도메인 메서드 호출

        return user;
    }

    // 포인트 환불
    public User refundPoint(long userId, long amount) {
        User user = getUserOrThrow(userId);
        user.refundPoint(amount);// 도메인 메서드 호출

        return user;
    }

    // 포인트 조회
    public long getPoint(long userId) {
        return getUserOrThrow(userId).getPoint();
    }

    /**
     * 유저 존재 여부를 확인하고 없으면 예외 발생.
     * 오직 ID 기반 검증에만 사용.
     */
    public User getUserOrThrow(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을수 없습니다."));
    }
}