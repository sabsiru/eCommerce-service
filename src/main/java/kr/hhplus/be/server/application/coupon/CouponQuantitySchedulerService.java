package kr.hhplus.be.server.application.coupon;

import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.CouponRepository;
import kr.hhplus.be.server.domain.coupon.CouponService;
import kr.hhplus.be.server.domain.coupon.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponQuantitySchedulerService {
    private final StringRedisTemplate redisTemplate;
    private final CouponRepository couponRepository;
    private final CouponService couponService;

    @Scheduled(cron = "0/30 * * * * *")
    @Transactional
    public void syncCouponRemainingQuantity() {
        List<Coupon> allCoupons = couponRepository.findActiveCoupons();


        for (Coupon coupon : allCoupons) {
            int limitCount = coupon.getLimitCount();
            String listKey = String.format("coupon:%d:inventory", coupon.getId());
            int remaining = Math.toIntExact(redisTemplate.opsForList().size(listKey));
            if (remaining == limitCount) {
                continue;
            }
            couponService.updateLimitCount(coupon.getId(), remaining);
        }
    }
}
