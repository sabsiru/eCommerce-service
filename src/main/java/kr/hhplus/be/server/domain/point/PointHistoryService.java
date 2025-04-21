package kr.hhplus.be.server.domain.point;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class PointHistoryService {

    private final PointHistoryRepository pointHistoryRepository;

    public PointHistory saveCharge(Long userId, long amount) {
        return save(PointHistory.charge(userId, amount));
    }

    public PointHistory saveUse(Long userId, long amount) {
        return save(PointHistory.use(userId, amount));
    }

    public PointHistory saveRefund(Long userId, long amount, Long orderId) {
        if (pointHistoryRepository.existsByOrderId(orderId)) {
            throw new IllegalStateException("이미 환불 처리된 주문입니다.");
        }
        return save(PointHistory.refund(userId, amount, orderId));
    }

    private PointHistory save(PointHistory history) {
        return pointHistoryRepository.save(history);
    }

    public List<PointHistory> getHistories(Long userId) {
        return pointHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}