# Redis 기반 쿠폰 선착순 발급 시스템

## 구현 방식 선택 배경

### List + Set 자료구조 조합을 선택한 이유

1. **중복 발급 방지**
   - Set 자료구조로 이미 발급받은 사용자 관리 (`ISSUED_USERS_KEY`)
   - O(1) 시간 복잡도로 빠른 중복 체크
   - SADD로 간단하게 발급 이력 관리

2. **재고 관리**
   - List 자료구조로 재고 수량 관리 (`INVENTORY_KEY`)
   - LPOP으로 atomic한 재고 차감
   - RPUSH로 간단한 재고 복구 처리

3. **트랜잭션 기반 동시성 제어**
   - 재고 검사부터 발급까지 원자성 보장
   - Race Condition 방지를 위한 격리성 확보
   - 롤백을 통한 일관성 유지
   - 트랜잭션 미사용 시 발생 가능한 문제:
      - 재고 검사와 차감 사이 경쟁 조건 발생
      - 부분 업데이트로 인한 데이터 불일치

4. **만료 처리**
   - Redis TTL 기능으로 쿠폰 만료 자동화
   - 재고와 발급 이력에 동일한 만료 시간 설정
   - 만료 전 hasKey로 명시적 검증

## 주요 구현 내용

### 1. 초기화
```java
public void initialize(Long couponId, int limitCount, LocalDateTime expirationAt) {
    final String inventoryKey   = String.format(INVENTORY_KEY, couponId);
    final String issuedUsersKey = String.format(ISSUED_USERS_KEY, couponId);
    final Duration ttl          = Duration.between(LocalDateTime.now(), expirationAt);

    redisTemplate.delete(inventoryKey);
    redisTemplate.delete(issuedUsersKey);
    for (int i = 0; i < limitCount; i++) {
        redisTemplate.opsForList().leftPush(inventoryKey, String.valueOf(i));
    }
    redisTemplate.expire(inventoryKey, ttl);
    redisTemplate.expire(issuedUsersKey, ttl);
}
```

### 2. 발급 처리
```java
public boolean issue(Long couponId, Long userId) {
    final String inventoryKey   = String.format(INVENTORY_KEY, couponId);
    final String issuedUsersKey = String.format(ISSUED_USERS_KEY, couponId);

    if (!redisTemplate.hasKey(inventoryKey)) {
        throw new IllegalStateException("만료된 쿠폰입니다.");
    }
    if (redisTemplate.opsForSet().isMember(issuedUsersKey, userId.toString())) {
        throw new IllegalStateException("이미 발급받은 사용자입니다.");
    }

    final String token = redisTemplate.opsForList().leftPop(inventoryKey);
    if (token == null) {
        throw new IllegalStateException("재고가 소진되었습니다.");
    }

    redisTemplate.opsForSet().add(issuedUsersKey, userId.toString());
    return true;
}
```

### 3. 트랜잭션 처리 (SessionCallback)
```java
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
```

#### SessionCallback 사용 이유
- **원자성 보장**: 재고 검사부터 발급까지 하나의 트랜잭션으로 처리
- **Race Condition 방지**: 동시 요청 시에도 안전한 처리 보장
- **일관성 유지**: 재고 차감과 발급 이력이 함께 처리됨

#### 주의사항
- Redis `MULTI`/`EXEC` 사이에서는 조회 결과를 즉시 확인할 수 없음
- 트랜잭션 실패 시 모든 작업이 롤백됨
- watch를 통한 낙관적 락 구현 가능

## 특징

### 장점
- 직관적인 데이터 모델링
- SessionCallback을 통한 트랜잭션으로 동시성 제어
- TTL로 자동 만료 처리
- O(1) 시간 복잡도로 빠른 처리

### 고려사항
- Redis 자체 명령어만으로는 동시성 보장 불가
- 재고 검사와 차감이 단일 트랜잭션으로 처리되어야 함
- 메모리 사용량이 재고량에 비례
- 네트워크 단절 시 트랜잭션 실패 가능성