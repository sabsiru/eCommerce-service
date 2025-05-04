# 캐시 전략 보고서

## ✅ 개요
인기상품 조회 API(`getPopularProducts`)에 다음 요소를 결합한 효율적 캐시 전략을 적용하였습니다.

1. **랜덤 TTL**  
2. **Cache-Aside 패턴 + Mutex 락**

---

## 1. 적용 서비스: 인기상품 조회
- **엔드포인트**: `GET /products/popular`  
- **비즈니스**: 최근 3일간 판매량 상위 5개 상품 집계  
- **목표**: 반복 집계 쿼리 부하 완화 및 대량 동시 호출 시 DB 과부하 방지

### 1.1 인기 상품 조회 쿼리

```
   QOrderItem orderItem = QOrderItem.orderItem;

        LocalDateTime to = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime from = to.minusDays(4);

        return queryFactory
                .select(Projections.constructor(PopularProductRow.class,
                        orderItem.productId,
                        orderItem.quantity.sum()))
                .from(orderItem)
                .where(orderItem.createdAt.goe(from)
                        .and(orderItem.createdAt.lt(to)))
                .groupBy(orderItem.productId)
                .orderBy(orderItem.quantity.sum().desc())
                .limit(5)
                .fetch();
```
---

## 2. 캐시 패턴 구성

### 2.1 Cache-Aside + Mutex 락 결합
```java
@Service
public class PopularProductService {
    
    // ...repo 주입
   
    @Cacheable(
      value   = "popularProducts",
      key     = "'top5'",
      unless  = "#result == null || #result.isEmpty()"
    )
    public List<PopularProductInfo> getPopularProducts() {
        return loadAndCache();
    }

    @DistributedLock(
      key       = "'popularProducts:top5'",
      waitTime  = 1,
      leaseTime = 2,
      timeUnit  = TimeUnit.SECONDS
    )
    public List<PopularProductInfo> loadAndCachePopularProducts() {
       List<PopularProductRow> rows = orderItemQueryRepository.findPopularProducts();
       return rows.stream()
               .map(row -> new PopularProductInfo(row.getProductId(), row.getTotalQuantity()))
               .toList();
    }
}
````

* **동작 순서**:

   1. 캐시 히트 시 `getPopularProducts()` 에서 빠르게 반환
   2. 캐시 미스 시 `loadAndCache()` 호출

      * 락 획득 → DB 집계 → 캐시 적재 → 락 해제
   3. 락 해제 후 나머지 요청은 캐시에서 즉시 읽어 반환

---

## 3. Expiration 전략: 랜덤 TTL

* **기본 TTL**: 10분
* **랜덤 오프셋**: ±1분
* **효과**: TTL 만료 시점 분산 → 동시 만료 확률 최소화

```java
long base = Duration.ofMinutes(10).getSeconds();
long jitter = ThreadLocalRandom.current().nextLong(-60, 61);
Duration ttl = Duration.ofSeconds(base + jitter);
configs.put("popularProducts", defaultConfig.entryTtl(ttl));
```

---

## 4. k6 부하 테스트 결과

### 4.1 캐시 적용 전

* 📦 총 요청 수: **1932**
* ❌ 실패율: **41.67%**

#### 응답 시간 (http\_req\_duration)

| 구간  | 시간 (ms)  |
| --- | -------- |
| 평균  | 10293.78 |
| 최소  | 307.48   |
| 최대  | 15684.99 |
| p50 | 10290.32 |
| p90 | 14662.20 |
| p95 | 15146.37 |
| p99 | 15493.73 |


### 4.2 캐시 적용 후

* 📦 총 요청 수: **141655**
* ❌ 실패율: **0.00%**

#### 응답 시간 (http\_req\_duration)

| 구간  | 시간 (ms) |
| --- | ------- |
| 평균  | 127.04  |
| 최소  | 1.95    |
| 최대  | 712.27  |
| p50 | 126.37  |
| p90 | 224.13  |
| p95 | 236.57  |
| p99 | 267.09  |

**부하 시나리오** (k6 설정):

```js
stages: [
  { duration: '30s', target: 100 },  // 점진 증가
  { duration: '1m', target: 300 },   // 고부하 유지
  { duration: '30s', target: 0 }     // 부하 제거
]
```

**분석 및 개선 포인트**

* **p99 응답 시간**: 캐시 적용 전 15493.73ms → 적용 후 267.09ms (SLA 충족)
* **평균 응답 시간**: 캐시 적용 전 10293.78ms → 적용 후 127.04ms
* **실패율**: 캐시 적용 전 41.67% → 적용 후 0.00%
* **전체 요청 수 증가**: 캐시 적용 전 1932 → 적용 후 141655 (높은 처리량 확보)

---

## 5. 결론

* **랜덤 TTL**로 만료 시점 분산
* **Cache-Aside + Mutex 락**으로 **Cache Stampede** 방어

---

[돌아가기](../../README.md)