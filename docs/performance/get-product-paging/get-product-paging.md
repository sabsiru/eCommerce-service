# 전체 상품 조회 (페이징)

## 상태
Accepted

## 맥락 (Context)
- 기능: 전체 상품 목록을 페이징하여 최신 등록순으로 조회하는 기능
- 목적: 사용자에게 최신 상품을 빠르게 제공하는 것이 목표
- 쿼리: `product` 테이블의 `created_at` 컬럼 기준으로 정렬된 상품을 페이지 단위로 조회
- 데이터 양: 약 50만 건

---

## 1. 기본 구현 (Offset + SELECT *)

### QueryDSL
```java
queryFactory
    .selectFrom(product)
    .orderBy(product.createdAt.desc())
    .offset(offset)
    .limit(limit)
    .fetch();
```

### SQL
```sql
SELECT * 
FROM product 
ORDER BY created_at DESC 
LIMIT 20 OFFSET 0;
```

### EXPLAIN
```text
1, SIMPLE, product, , ALL, , , , , 497658, 100, Using filesort
```

| 항목 | 설명 |
|------|------|
| type | `ALL`: 전체 테이블 스캔 |
| rows | `497658`: 모든 row 순회 |
| Extra | `Using filesort`: 메모리 정렬 발생 |

### k6 성능 테스트 결과
```text
📦 총 요청 수: 11,363
❌ 실패율: 0.00%

⏱️ 응답 시간 (http_req_duration)
- 평균: 1607.08 ms
- 최소: 14.48 ms
- 최대: 7310.81 ms
- p50: 1513.40 ms
- p90: 2953.56 ms
- p95: 3138.86 ms
- p99: 4679.86 ms ❌
```

---

## 2. 커서 기반 페이지네이션 (createdAt + id)

### QueryDSL
```java
if (cursorCreatedAt != null && cursorId != null) {
    condition.and(
        product.createdAt.lt(cursorCreatedAt)
            .or(product.createdAt.eq(cursorCreatedAt).and(product.id.lt(cursorId)))
    );
}
```

### 인덱스
```sql
CREATE INDEX idx_product_covering 
ON product(created_at DESC, id DESC, name, price);
```

### EXPLAIN
```text
1, SIMPLE, product, , index, idx_product_covering, 9, , 20, 100
```

| 항목 | 설명 |
|------|------|
| type | `index`: 인덱스만 읽음 |
| key | `idx_product_covering`: 정렬 인덱스 사용 |
| Extra | (없음): `Using filesort` 제거됨 |

### k6 성능 테스트 결과 (Covering Index 기준)
```text
📦 총 요청 수: 100,576
❌ 실패율: 0.00%

⏱️ 응답 시간 (http_req_duration)
- 평균: 178.67 ms
- 최소: 2.65 ms
- 최대: 1978.71 ms
- p50: 167.85 ms
- p90: 316.86 ms
- p95: 343.29 ms
- p99: 491.16 ms ❌
```

---

## 3. 최종안: Offset 방식 + 필드 축소 + 정렬 인덱스 적용

### QueryDSL
```java
.select(Projections.constructor(ProductSummaryRow.class,
    product.id,
    product.name,
    product.price
))
.from(product)
.orderBy(product.createdAt.desc())
.offset(offset)
.limit(limit)
.fetch();
```

### SQL
```sql
SELECT p.id, p.name, p.price
FROM product p
ORDER BY p.created_at DESC
LIMIT 20 OFFSET 0;
```

### 인덱스
```sql
CREATE INDEX idx_created_at ON product(created_at DESC);
```

### EXPLAIN
```text
1, SIMPLE, p, , index, idx_created_at, 9, , 20, 100
```

| 항목 | 설명 |
|------|------|
| type | `index`: 인덱스 정렬 순서대로 읽음 |
| key | `idx_created_at` |
| rows | 20: LIMIT 적용 후 적은 row만 순회 |
| Extra | (없음): `Using filesort` 제거됨 |

### k6 성능 테스트 결과 (최종안)
```text
📦 총 요청 수: 124,592
❌ 실패율: 0.00%

⏱️ 응답 시간 (http_req_duration)
- 평균: 144.30 ms
- 최소: 2.03 ms
- 최대: 616.80 ms
- p50: 147.59 ms
- p90: 252.48 ms
- p95: 267.99 ms
- p99: 283.92 ms ✅
```

---

## 결론

| 방식 | 평균 응답시간 | p99 응답시간 | SLA 만족 여부 |
|------|----------------|----------------|----------------|
| 기본 (select *) | 1607.08 ms | 4679.86 ms | ❌ |
| 커서 기반 | 178.67 ms | 491.16 ms | ❌ |
| ✅ 최종안 | 144.30 ms | 283.92 ms | ✅ **통과** |

- 커서 방식은 평균 응답시간 측면에서는 우수했으나, SLA 기준을 만족하지 못함(커서 방식은 다소 낯설어 파라미터나 설정이 잘못 되어있을수도 있음.)
- 최종안은 구조가 단순하고, 정렬 인덱스 + 필드 축소로 성능이 가장 우수함
- 따라서 전체 상품 페이징 조회 API는 **Offset 방식 + 정렬 인덱스 + 필드 축소 구조로 유지하기로 결정**

---

## 추적 정보

- 작성일: 2025-04-17
- 작성자: @영인

[돌아가기](../../../README.md)