package kr.hhplus.be.server.application.user;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.application.point.PointHistoryService;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserPointFacade {

    private final UserPointService userPointService;
    private final PointHistoryService pointHistoryService;

    public User chargePoint(Long userId, long amount) {

        User updated = userPointService.chargePoint(userId, amount);  // chargePoint() 메서드 호출
        pointHistoryService.saveCharge(userId, amount);
        return updated;
    }

    public User usePoint(Long userId, long amount) {
        // 포인트 사용 후 User 객체 반환
        User updated = userPointService.usePoint(userId, amount);
        // 포인트 내역 저장
        pointHistoryService.saveUse(userId, amount);
        return updated;
    }

    public User refundPoint(Long userId, long amount, Long orderId) {
        // 환불은 충전과 동일하게 처리
        User updated = userPointService.refundPoint(userId, amount);
        // 포인트 내역 저장
        pointHistoryService.saveRefund(userId, amount, orderId);
        return updated;
    }
}