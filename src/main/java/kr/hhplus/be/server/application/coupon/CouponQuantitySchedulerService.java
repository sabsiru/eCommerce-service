package kr.hhplus.be.server.application.coupon;

import kr.hhplus.be.server.domain.coupon.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponQuantitySchedulerService {
    private final StringRedisTemplate redisTemplate;
    private final CouponRepository couponRepository;
    private final CouponService couponService;
    private final UserCouponRepository userCouponRepository;

    @Scheduled(cron = "*/30 * * * * *")
    @Transactional
    public void syncCouponRemainingQuantity() {
        List<Coupon> allCoupons = couponRepository.findActiveCoupons();

        for (Coupon coupon : allCoupons) {
            int limitCount = coupon.getLimitCount();
            String listKey = String.format("coupon:%d:inventory", coupon.getId());
            int remaining = Math.toIntExact(redisTemplate.opsForList().size(listKey));
            log.info("쿠폰 ID: {}, 재고 수량: {}, 제한 수량: {}", coupon.getId(), remaining, limitCount);
            if (remaining == limitCount) {
                continue;
            }
            couponService.updateLimitCount(coupon.getId(), remaining);
        }
    }
}
