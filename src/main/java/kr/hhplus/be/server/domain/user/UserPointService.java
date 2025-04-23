package kr.hhplus.be.server.domain.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserPointService {

    private final UserRepository userRepository;

    public User chargePoint(long userId, long amount) {
        User user = getUserOrThrow(userId);
        user.chargePoint(amount);

        return user;
    }

    public User usePoint(long userId, long amount) {
        User user = getUserOrThrow(userId);
        user.usePoint(amount);

        return user;
    }

    public User refundPoint(long userId, long amount) {
        User user = getUserOrThrow(userId);
        user.refundPoint(amount);

        return user;
    }

    public long getPoint(long userId) {
        return getUserOrThrow(userId).getPoint();
    }

    public User getUserOrThrow(long userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을수 없습니다."));
    }
}