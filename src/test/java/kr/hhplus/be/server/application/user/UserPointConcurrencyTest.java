package kr.hhplus.be.server.application.user;

import kr.hhplus.be.server.domain.point.PointHistory;
import kr.hhplus.be.server.domain.point.PointHistoryRepository;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class UserPointConcurrencyTest {

    @Autowired
    UserPointFacade userPointFacade;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PointHistoryRepository pointHistoryRepository;

    @Test
    void 동시_포인트_사용_정상_처리_확인() throws InterruptedException {
        // given
        int initialPoint = 1000;
        User user = userRepository.save(User.create("동시성테스트", initialPoint));
        int threads = 10;
        int amount = 200;

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    userPointFacade.usePoint(user.getId(), amount);
                } catch (Exception e) {
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
        // when
        User updated = userRepository.findById(user.getId()).orElseThrow();
        List<PointHistory> histories = pointHistoryRepository.findByUserId(user.getId());
        // then: 락이 제대로 걸리면 1000/200=5건만 성공해 정확히 0원이 남고, 5건의 사용 이력만
        // 남는다. 범위(isBetween/isLessThanOrEqualTo)로만 검증하면 락이 일부만 걸리는 경우도
        // 우연히 통과할 수 있어 정확한 기대값으로 검증한다.
        int expectedSuccessCount = initialPoint / amount;
        assertThat(updated.getPoint()).isEqualTo(initialPoint - expectedSuccessCount * amount);
        assertThat(histories).hasSize(expectedSuccessCount);
    }

    @Test
    void 동시_포인트_충전_시_최대_보유_한도_초과_검증() throws InterruptedException {
        // given
        User user = userRepository.save(User.create("유저", 9_000_000));

        int chargeAmount = 500_000;
        int threadCount = 5;
        final int max_amount = 10_000_000;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    userPointFacade.chargePoint(user.getId(), chargeAmount);
                } catch (Exception ignored) {
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
        // then
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        List<PointHistory> histories = pointHistoryRepository.findByUserId(user.getId());

        assertThat(updatedUser.getPoint()).isLessThanOrEqualTo(max_amount);
        assertThat(histories.size()).isLessThanOrEqualTo(2);
    }

    @Test
    void 동시_포인트_환불_정상_처리_확인() throws InterruptedException {
        // given
        User user = userRepository.save(User.create("환불테스트유저", 1000));

        userPointFacade.usePoint(user.getId(), 200);

        int threads = 5;
        int refundAmount = 200;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        // when
        Long orderId = 1L;
        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    userPointFacade.refundPoint(user.getId(), refundAmount, orderId);
                } catch (Exception ignored) {
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);

        // then
        User updated = userRepository.findById(user.getId()).orElseThrow();
        List<PointHistory> histories = pointHistoryRepository.findByUserId(user.getId());

        assertThat(updated.getPoint()).isLessThanOrEqualTo(1000);

        assertThat(histories).hasSizeLessThanOrEqualTo(2);
    }

}