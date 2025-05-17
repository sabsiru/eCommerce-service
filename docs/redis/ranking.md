# Redis Ranking System 구현

## 개요
상품 판매량 집계를 위한 Redis Sorted Set 활용 시스템

## Redis 키 구조
- 키 패턴: `product:sales:daily:{yyyy-MM-dd}`
- 데이터 타입: Sorted Set (ZSET)
- 만료 기간: 4일 (TTL)

## 데이터 구조
```
ZSET member: 상품 ID (String)
ZSET score: 판매 수량 (Double)
```

## 주요 작업

### 1. 판매량 증가
```java
redisTemplate.opsForZSet().incrementScore(
    key,
    String.valueOf(productId),
    quantity
);
```
- `incrementScore`: Atomic 연산으로 동시성 보장
- 일별 판매량 자동 집계

### 2. TTL 설정
```java
redisTemplate.expire(key, TTL_DAYS, TimeUnit.DAYS);
```
- 데이터 보관 기간: 4일
- 자동 만료로 저장소 최적화

## 조회 예시
```java
// 상위 N개 상품 조회
redisTemplate.opsForZSet().reverseRange(key, 0, N-1);

// 특정 상품 순위 조회
redisTemplate.opsForZSet().reverseRank(key, productId);
```

## 성능 고려사항
- 시간복잡도: O(log(N))
- 메모리 사용: 일별 데이터만 유지
- 동시성: Redis atomic 연산 활용