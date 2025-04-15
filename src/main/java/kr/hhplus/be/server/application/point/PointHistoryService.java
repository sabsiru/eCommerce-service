package kr.hhplus.be.server.application.point;

import kr.hhplus.be.server.domain.point.PointHistory;
import kr.hhplus.be.server.domain.point.PointHistoryRepository;
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
        return save(PointHistory.refund(userId, amount, orderId));
    }
    private PointHistory save(PointHistory history) {
        return pointHistoryRepository.save(history);
    }

    public List<PointHistory> getHistories(Long userId) {
        return pointHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}