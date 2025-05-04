package kr.hhplus.be.server.application.redis;

import java.util.concurrent.TimeUnit;

public interface LockService {
    <T> T executeWithLock( String key,
                           long waitTime,
                           long leaseTime,
                           TimeUnit timeUnit,
                           LockExecutor<T> executor);
}