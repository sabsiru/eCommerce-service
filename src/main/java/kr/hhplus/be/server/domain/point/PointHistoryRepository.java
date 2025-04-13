package kr.hhplus.be.server.domain.point;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointHistoryRepository {
    PointHistory save(PointHistory history);
    List<PointHistory> findByUserId(Long userId);
}