package kr.hhplus.be.server.infrastructure.coupon;

import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.CouponService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class RedisCouponInventoryReaderTest {
    @Autowired
    private CouponService couponService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Test
    void 쿠폰_만료_시간이_지나면_인벤토리가_자동으로_삭제되고_발급시_예외가_발생한다() throws InterruptedException {
        // given
        String name = "테스트 쿠폰";
        int discountRate = 10;
        int maxDiscountAmount = 1000;
        LocalDateTime expirationAt = LocalDateTime.now().plusSeconds(2);
        int limitCount = 5;

        // when
        Coupon coupon = couponService.create(name, discountRate, maxDiscountAmount, expirationAt, limitCount);
        String inventoryKey = String.format("coupon:%d:inventory", coupon.getId());

        // then
        // 초기화 직후에는 발급 가능
        assertThat(redisTemplate.hasKey(inventoryKey)).isTrue();
        assertThat(redisTemplate.opsForList().size(inventoryKey)).isEqualTo(limitCount);
        assertThat(couponService.issue(1L, coupon.getId())).isNotNull();

        // 3초 대기 (만료 시간 이후)
        Thread.sleep(3000);

        // 만료 후에는 데이터가 없어야 함
        assertThat(redisTemplate.hasKey(inventoryKey)).isFalse();

        // issue 시도 시 만료 예외 발생
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> couponService.issue(1L, coupon.getId()));
        assertThat(exception.getMessage()).isEqualTo("만료된 쿠폰입니다.");
    }
}
