package kr.hhplus.be.server.application.coupon;

import kr.hhplus.be.server.domain.coupon.*;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CouponConcurrencyTest {

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CouponService couponService;

    @Test
    void 여러_유저가_동시_쿠폰_발급시_1건발급_테스트() throws InterruptedException {
        // given
        Coupon coupon = couponService.create("테스트쿠폰", 10, 1000, LocalDateTime.now().plusDays(1), 1);

        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            User user = userRepository.save(User.create("user" + i, 0));
            long userId = user.getId();

            executorService.submit(() -> {
                try {
                    couponService.issue(userId, coupon.getId());
                } catch (Exception e) {
                    // 무시
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.SECONDS);
        // then
        List<UserCoupon> issuedCoupons = userCouponRepository.findAllByCouponId(coupon.getId());
        assertThat(issuedCoupons).hasSize(1);
    }

    @Test
    void 동시_쿠폰_발급_시_한_유저에게_중복_발급_제한됨() throws Exception {
        // given
        User user = userRepository.save(User.create("중복테스트유저", 0));
        Coupon coupon = couponService.create("테스트쿠폰", 10, 1000, LocalDateTime.now().plusDays(1), 10);

        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    couponService.issue(user.getId(), coupon.getId());
                } catch (Exception e) {

                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.SECONDS);
        // then
        List<UserCoupon> results = userCouponRepository.findAllByUserId(user.getId());
        assertThat(results).hasSize(1);
    }
}