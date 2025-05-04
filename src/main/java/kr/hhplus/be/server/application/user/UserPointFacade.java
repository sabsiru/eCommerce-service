package kr.hhplus.be.server.application.user;

import kr.hhplus.be.server.domain.point.PointHistoryService;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserPointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class UserPointFacade {

    private final UserPointService userPointService;
    private final PointHistoryService pointHistoryService;

    public User chargePoint(Long userId, int amount) {
        User updated = userPointService.chargePoint(userId, amount);
        pointHistoryService.saveCharge(userId, amount);
        return updated;
    }

    @Transactional
    public User usePoint(Long userId, int amount) {
        User updated = userPointService.usePoint(userId, amount);
        pointHistoryService.saveUse(userId, amount);
        return updated;
    }

    @Transactional
    public User refundPoint(Long userId, int amount, Long orderId) {
        User updated = userPointService.refundPoint(userId, amount);
        pointHistoryService.saveRefund(userId, amount, orderId);
        return updated;
    }
}