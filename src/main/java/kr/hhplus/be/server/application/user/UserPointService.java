package kr.hhplus.be.server.application.user;

import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UserPointService {

    private final UserRepository userRepository;

    // 포인트 충전
    public User chargePoint(long userId, long amount) {
        User user = getUserOrThrow(userId);
        User chargedPoint = user.chargePoint(amount);

        return userRepository.save(chargedPoint);
    }

    // 포인트 사용
    public User usePoint(long userId, long amount) {
        User user = getUserOrThrow(userId);
        User usedPoint = user.usePoint(amount);// 도메인 메서드 호출

        return userRepository.save(usedPoint);
    }

    // 포인트 환불
    public User refundPoint(long userId, long amount) {
        User user = getUserOrThrow(userId);
        User refundedPoint = user.refundPoint(amount);// 도메인 메서드 호출

        return userRepository.save(refundedPoint);
    }

    // 포인트 조회
    public long getPoint(long userId) {
        User user = getUserOrThrow(userId);
        return user.getPoint();
    }

    // 공통 헬퍼 메서드: userId로 유저를 조회하고, 없으면 예외 발생
    private User getUserOrThrow(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을수 없습니다."));
    }
}