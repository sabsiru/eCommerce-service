package kr.hhplus.be.server.domain.user;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
@Transactional
class UserIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void 유저_생성_및_포인트_충전_저장_조회() {
        // given
        User user = User.create("테스트유저", 0);
        User saved = userRepository.save(user);

        // when
        saved.charge(1000);
        userRepository.save(saved);

        // then
        User found = userRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getPoint()).isEqualTo(1000);
        assertThat(found.getName()).isEqualTo("테스트유저");
    }
    @Test
    void 음수_금액_충전시_예외() {
        // given
        User user = userRepository.save(User.create("음수충전", 0));

        // expect
        assertThatThrownBy(() -> {
            user.charge(-5000);
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("충전 금액은 0보다 커야 합니다.");
    }

    @Test
    void 최대_1회_충전_금액_초과시_예외() {
        // given
        User user = userRepository.save(User.create("초과충전", 0));

        // expect
        assertThatThrownBy(() -> {
            user.charge(2_000_000);
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("1회 충전 금액은 `1,000,000원` 입니다.");
    }

    @Test
    void 보유포인트_최대한도_초과시_예외() {
        // given
        User user = userRepository.save(User.create("초과보유", 9_800_000));

        // expect
        assertThatThrownBy(() -> {
            user.charge(300_000);
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("최대 충전 한도는 `10,000,000원` 입니다.");
    }

    @Test
    void 포인트_정상_사용() {
        // given
        User user = userRepository.save(User.create("정상사용자", 5000));

        // when
        user.use(3000);
        userRepository.save(user);

        // then
        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPoint()).isEqualTo(2000);
    }

    @Test
    void 사용_후_포인트_정확히_조회됨() {
        // given
        User user = userRepository.save(User.create("조회확인", 7000));

        // when
        user.use(2000);
        userRepository.save(user);

        // then
        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPoint()).isEqualTo(5000);
    }

    @Test
    void 보유포인트_보다_많이_사용하면_예외발생() {
        // given
        User user = userRepository.save(User.create("예외사용자", 1000));

        // expect
        assertThatThrownBy(() -> {
            user.use(2000);
        }).isInstanceOf(IllegalStateException.class)
                .hasMessage("포인트가 부족합니다.");
    }
}