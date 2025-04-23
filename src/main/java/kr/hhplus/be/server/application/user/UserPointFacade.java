package kr.hhplus.be.server.application.user;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.domain.point.PointHistoryService;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserPointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Transactional
public class UserPointFacade {

    private final UserPointService userPointService;
    private final PointHistoryService pointHistoryService;

    public User chargePoint(Long userId, int amount) {
        User updated = userPointService.chargePoint(userId, amount);
        pointHistoryService.saveCharge(userId, amount);
        return updated;
    }

    public User usePoint(Long userId, int amount) {
        User updated = userPointService.usePoint(userId, amount);
        pointHistoryService.saveUse(userId, amount);
        return updated;
    }

    public User refundPoint(Long userId, int amount, Long orderId) {
        User updated = userPointService.refundPoint(userId, amount);
        pointHistoryService.saveRefund(userId, amount, orderId);
        return updated;
    }
}