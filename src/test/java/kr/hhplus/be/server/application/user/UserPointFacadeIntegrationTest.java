package kr.hhplus.be.server.application.user;

import kr.hhplus.be.server.domain.point.PointHistory;
import kr.hhplus.be.server.domain.point.PointHistoryRepository;
import kr.hhplus.be.server.domain.point.PointHistoryType;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class UserPointFacadeIntegrationTest {

    @Autowired
    private UserPointFacade userPointFacade;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PointHistoryRepository pointHistoryRepository;

    @Test
    void 포인트_충전_정상_히스토리_기록됨() {
        // given
        User user = userRepository.save(User.create("충전유저", 0));

        // when
        userPointFacade.chargePoint(user.getId(), 5000);

        // then
        User found = userRepository.findById(user.getId()).orElseThrow();
        assertThat(found.getPoint()).isEqualTo(5000);

        List<PointHistory> histories = pointHistoryRepository.findByUserId(user.getId());
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getAmount()).isEqualTo(5000);
        assertThat(histories.get(0).getType()).isEqualTo(PointHistoryType.CHARGE);
    }

    @Test
    void 포인트_사용_정상_히스토리_기록됨() {
        // given
        User user = userRepository.save(User.create("사용유저", 10_000));

        // when
        userPointFacade.usePoint(user.getId(), 4000);

        // then
        User found = userRepository.findById(user.getId()).orElseThrow();
        assertThat(found.getPoint()).isEqualTo(6000);

        List<PointHistory> histories = pointHistoryRepository.findByUserId(user.getId());
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getAmount()).isEqualTo(4000);
        assertThat(histories.get(0).getType()).isEqualTo(PointHistoryType.USE);
    }

    @Test
    void 포인트_부족시_예외발생_히스토리_저장되지_않음() {
        // given
        User user = userRepository.save(User.create("예외유저", 1000));

        // expect
        assertThatThrownBy(() -> userPointFacade.usePoint(user.getId(), 3000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("포인트가 부족합니다.");

        List<PointHistory> histories = pointHistoryRepository.findByUserId(user.getId());
        assertThat(histories).isEmpty();
    }

    @Test
    void 충전_후_즉시_사용_정상_포인트_및_히스토리_누적() {
        // given
        User user = userRepository.save(User.create("연속유저", 0));

        // when
        userPointFacade.chargePoint(user.getId(), 10000);
        userPointFacade.usePoint(user.getId(), 4000);

        // then
        User found = userRepository.findById(user.getId()).orElseThrow();
        assertThat(found.getPoint()).isEqualTo(6000);

        List<PointHistory> histories = pointHistoryRepository.findByUserId(user.getId());
        assertThat(histories).hasSize(2);
        assertThat(histories).extracting(PointHistory::getType)
                .containsExactlyInAnyOrder(PointHistoryType.CHARGE, PointHistoryType.USE);
    }

    @Test
    void 중복_충전시_누적_포인트_및_히스토리_정확히_기록됨() {
        // given
        User user = userRepository.save(User.create("중복충전유저", 0));

        // when
        userPointFacade.chargePoint(user.getId(), 5000);
        userPointFacade.chargePoint(user.getId(), 2000);

        // then
        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPoint()).isEqualTo(7000);

        List<PointHistory> histories = pointHistoryRepository.findByUserId(user.getId());
        assertThat(histories).hasSize(2);
        assertThat(histories).allMatch(h -> h.getType() == PointHistoryType.CHARGE);
    }

    @Test
    void 충전과_사용을_반복하면_포인트_정확히_계산되고_히스토리도_누적된다() {
        // given
        User user = userRepository.save(User.create("반복유저", 0));

        // when
        userPointFacade.chargePoint(user.getId(), 10000);
        userPointFacade.usePoint(user.getId(), 3000);
        userPointFacade.chargePoint(user.getId(), 2000);
        userPointFacade.usePoint(user.getId(), 1000);

        // then
        User found = userRepository.findById(user.getId()).orElseThrow();
        assertThat(found.getPoint()).isEqualTo(8000);

        List<PointHistory> histories = pointHistoryRepository.findByUserId(user.getId());
        assertThat(histories).hasSize(4);
        assertThat(histories).extracting(PointHistory::getType)
                .containsExactlyInAnyOrder(
                        PointHistoryType.CHARGE,
                        PointHistoryType.USE,
                        PointHistoryType.CHARGE,
                        PointHistoryType.USE
                );
    }

    @Test
    void 존재하지_않는_유저ID로_사용하면_예외발생() {
        // when & then
        assertThatThrownBy(() -> userPointFacade.usePoint(99999L, 1000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("유저를 찾을수 없습니다.");
    }

    @Test
    void 포인트_0원_충전_시_예외() {
        User user = userRepository.save(User.create("제로충전유저", 1000));

        assertThatThrownBy(() -> userPointFacade.chargePoint(user.getId(), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("충전 금액은 0보다 커야 합니다.");
    }

    @Test
    void 중간에_예외가_발생하면_포인트와_히스토리_모두_반영되지_않는다() {
        // given
        User user = userRepository.save(User.create("롤백유저", 1000));

        try {
            // when - 예외 유도
            userPointFacade.usePoint(user.getId(), 9999999);
        } catch (Exception ignored) {
        }

        // then
        User found = userRepository.findById(user.getId()).orElseThrow();
        assertThat(found.getPoint()).isEqualTo(1000);

        List<PointHistory> histories = pointHistoryRepository.findByUserId(user.getId());
        assertThat(histories).isEmpty();
    }

    @Test
    void 포인트_충전_시_최대_보유한도_초과하면_예외발생_및_히스토리_기록되지_않음() {
        // given
        User user = userRepository.save(User.create("제한유저", 9_900_000));

        // when
        assertThatThrownBy(() -> userPointFacade.chargePoint(user.getId(), 200_000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("최대 충전 한도는 `10,000,000원` 입니다.");

        // then
        List<PointHistory> histories = pointHistoryRepository.findByUserId(user.getId());
        assertThat(histories).isEmpty();
    }

    @Test
    void 포인트_부족시_예외_발생하고_포인트_및_히스토리_변경없음() {
        // given
        User user = userRepository.save(User.create("부족유저", 500));

        // when
        assertThatThrownBy(() -> userPointFacade.usePoint(user.getId(), 1000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("포인트가 부족합니다.");

        // then
        User found = userRepository.findById(user.getId()).orElseThrow();
        assertThat(found.getPoint()).isEqualTo(500);

        List<PointHistory> histories = pointHistoryRepository.findByUserId(user.getId());
        assertThat(histories).isEmpty();
    }
}
