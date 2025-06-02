package kr.hhplus.be.server.application.coupon;

import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.CouponService;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.domain.coupon.UserCouponRepository;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        properties = {
                "spring.kafka.consumer.group-id=coupon-issuer",
                "spring.kafka.consumer.auto-offset-reset=earliest"
        }
)
@EmbeddedKafka(
        partitions = 1,
        topics = {"inside.coupon.v1.issued"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@ActiveProfiles("local")
class CouponIssueKafkaTest {

    @Autowired
    private CouponFacade couponFacade;

    @Autowired
    private CouponService couponService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    private Coupon coupon;
    private int limitCount = 1;
    @BeforeEach
    void setup() {
        coupon = couponService.create("테스트 쿠폰", 10, 3000, LocalDateTime.now().plusDays(3), limitCount);
        IntStream.range(0, 10).forEach(i ->
                userRepository.save(User.create("user" + i, 0))
        );
    }

    @Test
    void whenConcurrentAsyncIssue_thenOnlyLimitCountIssued() {
        int threadCount = 10;
        ExecutorService ex = Executors.newFixedThreadPool(threadCount);

        userRepository.findAll().forEach(user ->
                ex.submit(() -> couponFacade.issueAsync(user.getId(), coupon.getId()))
        );
        ex.shutdown();

        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    List<UserCoupon> issued = userCouponRepository.findAllByCouponId(coupon.getId());
                    assertThat(issued).hasSize(limitCount);
                });
    }
}