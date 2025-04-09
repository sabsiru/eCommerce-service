package kr.hhplus.be.server.application.point;

import kr.hhplus.be.server.domain.point.PointHistory;
import kr.hhplus.be.server.domain.point.PointHistoryRepository;
import kr.hhplus.be.server.domain.point.PointHistoryType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointHistoryServiceTest {

    @Mock
    private PointHistoryRepository repository;

    @InjectMocks
    private PointHistoryService service;

    @Test
    void 포인트_충전_기록을_저장한다() {
        // given
        Long userId = 1L;
        int amount = 10000;

        // when
        service.saveCharge(userId, amount);

        // then
        verify(repository).save(argThat(history ->
                history.userId() == userId &&
                        history.amount() == amount &&
                        history.type() == PointHistoryType.CHARGE
        ));
    }

    @Test
    void 포인트_사용_기록을_저장한다() {
        Long userId = 1L;
        int amount = 5000;
        int pointAfter = 10000;

        service.saveUse(userId, amount);

        verify(repository).save(argThat(history ->
                history.userId() == (userId) &&
                        history.amount() == amount &&
                        history.type() == PointHistoryType.USE
        ));
    }

    @Test
    void 포인트_환불_기록을_저장한다() {
        Long userId = 1L;
        int amount = 3000;
        int pointAfter = 13000;
        Long orderId = 999L;

        service.saveRefund(userId, amount, orderId);

        verify(repository).save(argThat(history ->
                history.userId() == (userId) &&
                        history.amount() == amount &&
                        history.type() == PointHistoryType.REFUND &&
                        history.relatedOrderId().equals(orderId)
        ));
    }

    @Test
    void 포인트_기록을_유저아이디로_조회한다() {
        // given
        Long userId = 1L;
        List<PointHistory> histories = List.of(
                PointHistory.charge(userId, 10000),
                PointHistory.use(userId, 5000)
        );
        when(repository.findByUserId(userId)).thenReturn(histories);

        // when
        List<PointHistory> result = service.getHistories(userId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).type()).isEqualTo(PointHistoryType.CHARGE);
        assertThat(result.get(1).type()).isEqualTo(PointHistoryType.USE);
    }
}