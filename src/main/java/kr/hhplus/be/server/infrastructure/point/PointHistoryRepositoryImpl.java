package kr.hhplus.be.server.infrastructure.point;

import kr.hhplus.be.server.domain.point.PointHistory;
import kr.hhplus.be.server.domain.point.PointHistoryRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PointHistoryRepositoryImpl implements PointHistoryRepository {
    @Override
    public PointHistory save(PointHistory history) {
        return null;
    }

    @Override
    public List<PointHistory> findByUserId(Long userId) {
        return List.of();
    }
}
