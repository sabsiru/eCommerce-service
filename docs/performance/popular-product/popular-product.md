# 인기 상품 조회

## 상태
Accepted

## 맥락 (Context)
- 기능: 최근 3일간 인기 상품 조회
- 목적: 사용자가 가장 많이 주문한 상품을 상위 5개까지 보여주는 기능
- 쿼리: `order_item` 테이블을 기준으로 `product_id`별로 `SUM(quantity)` 후 정렬
- 조건: `created_at >= :fromDate AND created_at < :toDate`
- 더미 데이터 50만 건 기준, productId는 1~1000 범위로 고르게 분포시킴
- 고부하 상황에서 심각한 지연 발생하여 병목 원인 분석 진행 중

## 진행 내용 (Progress)
- QueryDSL 기반으로 다음 쿼리 실행됨:

```sql
SELECT product_id, SUM(quantity) AS total_quantity  
FROM order_item  
WHERE created_at >= '2025-04-14 00:00:00'  
  AND created_at < '2025-04-17 00:00:00'  
GROUP BY product_id  
ORDER BY total_quantity DESC  
LIMIT 5;
```

- 단순 인덱스(`created_at`, `product_id`)로는 `filesort`, `temporary` 제거 불가능
- `SUM(quantity)` 기반 정렬은 인덱스로 커버 불가

## 성능 테스트 결과 (k6 기반)

- 사용자 시나리오:

```js
stages: [  
  { duration: '30s', target: 100 },  
  { duration: '1m', target: 300 },  
  { duration: '30s', target: 0 },    
]
```

- 테스트 전 쿼리 기준 지표 요약:

```text
📦 총 요청 수: 2333  
❌ 실패율: 7.24%

⏱️ 응답 시간 (http_req_duration)
- 평균: 8474.78 ms
- 최소: 155.91 ms
- 최대: 16080.18 ms
- p50: 9292.86 ms
- p90: 14303.80 ms
- p95: 15007.94 ms
- p99: 15680.52 ms
```

> SLA 기준: **p99 < 300ms** 권장

## 구현 전략
- `popular_product_summary` 요약 테이블 설계
  - 필드: `product_id`, `summary_date`, `total_quantity`
  - 매일 `01:00` 기준 배치 작업으로 요약 데이터 갱신
- 구현 방식
  - 기존 집계 → 삭제 (`deleteAll()`)
  - 새로운 결과 저장 (`saveAll()`로 상위 5개만 저장)
  - summary_date는 **기록용 필드**로만 사용되며, 현재는 쿼리 조건에 포함하지 않음
- API 변경
  - 인기 상품 조회 시 기존 `order_item` 기준 → `summary` 테이블 기준으로 조회
  - Repository 메서드는 `findAll()`만 사용 (5개만 저장되므로 별도 조건 불필요)

## 성능 개선 이후 결과

- 배치 실행 후 요약 테이블 기반 조회로 변경
- 개선 후 성능 테스트 결과:

```text
📦 총 요청 수: 126124
❌ 실패율: 0.00%

⏱️ 응답 시간 (http_req_duration)
- 평균: 142.56 ms
- 최소: 2.24 ms
- 최대: 726.44 ms
- p50: 143.86 ms
- p90: 243.66 ms
- p95: 258.86 ms
- p99: 302.39 ms
```

> SLA 기준(p99 < 300ms) 근접 달성. 대부분 요청에서 안정적인 응답 확보.

## 결론
- 인덱스 최적화만으로는 병목 해소 불가
- 조회 전용 요약 테이블 도입이 가장 효과적
- 장점: 조회 성능 비약적 개선, 부하 시나리오에서도 안정적
- 단점: 실시간 데이터 반영 어려움, 운영 복잡도 증가


## 추적 정보
- 작성일: 2025-04-17
- 작성자: @영인


[돌아가기](../../../README.md)