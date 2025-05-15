package kr.hhplus.be.server.infrastructure.coupon;

import kr.hhplus.be.server.domain.coupon.CouponInventoryReader;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class RedisCouponInventoryReader implements CouponInventoryReader {
    private final RedisTemplate<String, String> redisTemplate;
    private static final String INVENTORY_KEY    = "coupon:%d:inventory";
    private static final String ISSUED_USERS_KEY = "coupon:%d:issued_users";

    public RedisCouponInventoryReader(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void initialize(Long couponId, int limitCount, LocalDateTime expirationAt) {
        final String inventoryKey    = String.format(INVENTORY_KEY, couponId);
        final String issuedUsersKey  = String.format(ISSUED_USERS_KEY, couponId);
        final Duration ttl           = Duration.between(LocalDateTime.now(), expirationAt);

        redisTemplate.delete(inventoryKey);
        redisTemplate.delete(issuedUsersKey);
        for (int i = 0; i < limitCount; i++) {
            redisTemplate.opsForList().leftPush(inventoryKey, String.valueOf(i));
        }
        redisTemplate.expire(inventoryKey, ttl);
        redisTemplate.expire(issuedUsersKey, ttl);
    }

   @Override
   public boolean issue(Long couponId, Long userId) {
       return redisTemplate.execute(new SessionCallback<>() {
           @Override
           public Boolean execute(RedisOperations operations) {
               String inventoryKey = String.format(INVENTORY_KEY, couponId);
               String issuedUsersKey = String.format(ISSUED_USERS_KEY, couponId);

               if (!operations.hasKey(inventoryKey)) {
                   throw new IllegalStateException("만료된 쿠폰입니다.");
               }

               if (Boolean.TRUE.equals(operations.opsForSet().isMember(issuedUsersKey, userId.toString()))) {
                   throw new IllegalStateException("이미 발급받은 사용자입니다.");
               }

               operations.multi();
               operations.opsForList().leftPop(inventoryKey);
               operations.opsForSet().add(issuedUsersKey, userId.toString());

               List<Object> results = operations.exec();
               if (results.get(0) == null) {
                   throw new IllegalStateException("재고가 소진되었습니다.");
               }

               return true;
           }
       });
   }

    @Override
    public void release(Long couponId, Long userId) {
        String inventoryKey = String.format(INVENTORY_KEY, couponId);
        String issuedUsersKey = String.format(ISSUED_USERS_KEY, couponId);
        redisTemplate.opsForSet().remove(issuedUsersKey, userId.toString());
        redisTemplate.opsForList().rightPush(inventoryKey, "0");
    }
}
