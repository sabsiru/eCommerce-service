# RedisTemplate vs Redisson 기반 분산락 설계 비교

## ✅ 설계 목적

Redis 기반 분산락을 구현할 때, RedisTemplate 단독 사용과 Redisson 사용 시의 구현 복잡성과 안정성 차이를 분석하여, Redisson 도입 이유를 명확히 한다.

---

## ✅ RedisTemplate 단독으로 락 구현 시

- `SETNX (setIfAbsent)`로 락 직접 획득
- TTL 직접 설정 필요 (expire 명령 별도 호출)
- 락 해제 시 Lua Script를 사용해야 정확성 보장
- 락 만료 자동 연장 기능 없음
- 락 해제 알림(Pub/Sub) 직접 구현 필요
- AOP 기반 공통처리(@DistributedLock) 직접 설계 필요
- 데드락 방어 로직 수동으로 작성 필요

---

## ✅ Redisson 사용 시

- `RLock` 객체를 통한 락 획득/해제 제공
- TTL 및 Lease Extension(자동 연장) 기본 지원
- 락 해제는 안전하게 atomic하게 처리됨
- Pub/Sub 기반 알림 자동 관리
- @RLock 등 AOP 기반 락 처리 제공
- 데드락 방어 및 서버 다운시 복구 지원
- 다양한 락 패턴(ReadWriteLock, FairLock 등) 기본 지원

---

## ✅ 기능별 비교

| 항목 | RedisTemplate | Redisson |
|:---|:---|:---|
| 락 획득 | 수동 구현 필요 | 제공됨 |
| TTL 설정 | 직접 호출 | 자동 처리 |
| 락 해제 정확성 | Lua Script 필요 | 자동 보장 |
| 만료 연장 | 직접 작성 필요 | 자동 연장 |
| AOP 지원 | 없음 | 제공(@RLock) |
| 데드락 방지 | 직접 처리 | 내장 처리 |

---

## ✅ 최종 판단

- **Redisson**은 락의 복잡한 생명주기(획득, 유지, 해제)를 모두 내장하고 있어 개발자가 락에 대해 직접 신경 쓸 필요가 없다.
- **RedisTemplate**은 모든 것을 직접 구현해야 하며, 안정성을 보장하기 위해 추가 개발비용이 크다.

---

# Redis 직접 구현 vs Redisson 라이브러리 기반 분산락 비교

---

## ✅ 개요

분산 시스템에서는 여러 서버 인스턴스가 동일한 자원에 접근할 수 있다.  
이를 제어하기 위해 분산락(Distributed Lock)이 필요하다.  
Redis를 이용한 분산락 구현 방법은 다음 두 가지가 있다.

- RedisTemplate을 사용하여 직접 락을 구현하는 방법
- Redisson 라이브러리를 활용하는 방법

---

## ✅ RedisTemplate 직접 구현

### 특징

- 락 획득/해제 로직을 직접 개발해야 한다.
- TTL 설정, 소유자 검증, 데드락 방지까지 모두 수동 구현이 필요하다.
- 장애 발생 시 처리, 재시도 로직까지 개발자가 직접 설계해야 한다.

### 락 획득 예시
```java
Boolean acquired = redisTemplate.opsForValue()
.setIfAbsent("lock:" + key, uuid, Duration.ofSeconds(5));

if (!Boolean.TRUE.equals(acquired)) {
    throw new IllegalStateException("락 획득 실패");
}
```

### 락 해제 예시
```java
String script = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
"return redis.call('del', KEYS[1]) else return 0 end";

redisTemplate.execute(
    new DefaultRedisScript<>(script, Long.class),
    Collections.singletonList("lock:" + key),
    uuid
);
```
---

## ✅ Redisson 라이브러리 활용

### 특징

- 락 획득, TTL 자동 연장, 소유자 검증, 장애 복구 기능이 기본 제공된다.
- tryLock, unlock 등 API만 호출하면 대부분의 분산락 기능을 사용할 수 있다.
- 복잡한 레이스 컨디션 문제를 라이브러리 수준에서 해결할 수 있다.

### 락 획득 및 해제 예시
```java
RLock lock = redissonClient.getLock("lock:" + key);

    try {
        if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
        // 비즈니스 로직 실행
            } else {
            throw new IllegalStateException("락 획득 실패");
            }
        } finally {
        if (lock.isHeldByCurrentThread()) {
        lock.unlock();
        }
    }
```
---

## ✅ RedisTemplate vs Redisson 비교 요약

| 항목 | RedisTemplate 직접 구현 | Redisson 활용 |
|:---|:---|:---|
| 락 획득/해제 구현 | 직접 SETNX + Lua Script 작성 | tryLock/unlock API 제공 |
| TTL 자동 연장 | 직접 구현해야 함 | 기본 제공 |
| 데드락 방지 | Pub/Sub 직접 설계 필요 | 자동 내장 처리 |
| 장애 복구 | 별도 구현 필요 | Redisson 자체 처리 |
| 개발 난이도 | 높음 | 낮음 |
| 운영 안정성 | 개발자 설계 품질에 따라 다름 | 검증된 수준 |

---

## ✅ 결론

- 단순한 학습이나 직접 제어가 필요한 경우에는 RedisTemplate 직접 구현도 가능하다.
- 그러나 실무에서는 안정성, 편의성, 유지보수성을 고려해 Redisson 사용이 일반적이다.
- 특히 Redisson은 락 기능 외에도 분산 환경에서 필요한 다양한 부가기능을 제공한다.