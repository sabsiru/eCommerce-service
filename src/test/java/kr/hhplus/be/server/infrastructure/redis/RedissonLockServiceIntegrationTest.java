package kr.hhplus.be.server.infrastructure.redis;

import kr.hhplus.be.server.application.redis.LockService;
import kr.hhplus.be.server.application.redis.LockExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

@SpringBootTest
class RedissonLockServiceIntegrationTest {

    @Autowired
    private LockService lockService;

    private ExecutorService executor;

    @AfterEach
    void tearDown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
    }

    @Test
    void 주어진_동시_경쟁에서_동시에_락을_획득하면_하나만_실행된다() throws InterruptedException {
        String key = "redisson:test:concurrent";
        AtomicInteger counter = new AtomicInteger();

        executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch done  = new CountDownLatch(2);

        for (int i = 0; i < 2; i++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    ready.await();
                    lockService.executeWithLock(
                            key, 0, 5, TimeUnit.SECONDS,
                            (LockExecutor<Void>) () -> {
                                counter.incrementAndGet();
                                return null;
                            }
                    );
                } catch (Throwable ignored) {
                } finally {
                    done.countDown();
                }
            });
        }

        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            done.await();
            assertThat(counter.get()).isEqualTo(1);
        });
    }

    @Test
    void 주어진_락_보유시간_만료후_두번째_스레드가_락을_획득한다() throws InterruptedException {
        String key = "redisson:test:lease";
        AtomicInteger order = new AtomicInteger();

        executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        executor.submit(() -> {
            try {
                lockService.executeWithLock(
                        key, 5, 1, TimeUnit.SECONDS,
                        (LockExecutor<Void>) () -> {
                            order.set(1);

                            try {
                                Thread.sleep(1500);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            return null;
                        }
                );
            } catch (Throwable ignored) {
            } finally {
                latch.countDown();
            }
        });

        executor.submit(() -> {
            try {

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                lockService.executeWithLock(
                        key, 5, 1, TimeUnit.SECONDS,
                        (LockExecutor<Void>) () -> {
                            order.compareAndSet(1, 2);
                            return null;
                        }
                );
            } catch (Throwable ignored) {
            } finally {
                latch.countDown();
            }
        });

        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            latch.await();
            assertThat(order.get()).isEqualTo(2);
        });
    }
}