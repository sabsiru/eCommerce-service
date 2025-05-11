package kr.hhplus.be.server.application.redis;

@FunctionalInterface
public interface LockExecutor<T> {
    T execute();
}