package kr.hhplus.be.server.infrastructure.redis;

import kr.hhplus.be.server.application.redis.LockExecutor;
import kr.hhplus.be.server.application.redis.LockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedissonLockServiceImpl implements LockService {

    private final RedissonClient redissonClient;

    @Override
    public <T> T executeWithLock(String key, long waitTime, long leaseTime, TimeUnit timeUnit, LockExecutor<T> executor) {
        RLock lock = redissonClient.getLock(key);

        try {
            boolean available = lock.tryLock(waitTime, leaseTime, timeUnit);
            if (!available) {
                throw new IllegalStateException("요청이 몰려 처리할 수 없습니다. 잠시 후 다시 시도해주세요.");
            }

            return executor.execute();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("요청 처리 중 문제가 발생했습니다. 잠시 후 다시 시도해주세요.");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

}
