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
        PointHistory history = PointHistory.charge(userId, amount);
        return pointHistoryRepository.save(history);
    }

    public PointHistory saveUse(Long userId, long amount) {
        PointHistory history = PointHistory.use(userId, amount);
        return pointHistoryRepository.save(history);
    }

    public PointHistory saveRefund(Long userId, long amount, Long orderId) {
        PointHistory history = PointHistory.refund(userId, amount, orderId);
        return pointHistoryRepository.save(history);
    }

    public List<PointHistory> getHistories(Long userId) {
        return pointHistoryRepository.findByUserId(userId);
    }
}