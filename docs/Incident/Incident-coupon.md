부하 테스트 대상 선정:
- **API 이름**: 선착순 쿠폰 발급 (`/coupons/{userId}/issue-async?couponId={couponId}`)
- **선정 이유**:
    1. **다중 컴포넌트 의존성**
        - 해당 API는 Redis(재고 관리 및 중복 검사), Kafka(비동기 메시징), MySQL(DB 저장) 등 여러 시스템이 결합되어 있어, 하나의 컴포넌트라도 장애가 발생하면 전체 워크플로우가 중단될 위험이 큼
    2. **순간 트래픽 집중 가능성**
        - 프로모션이나 이벤트 시작 시점에 수만 명의 사용자가 동시에 쿠폰을 발급받으려 하기 때문에, 아주 짧은 시간에 트래픽이 폭증하며 시스템 부하가 급격히 상승함
    3. **데이터 정합성 요구**
        - 동시 요청이 몰리는 상황에서도 “동일 사용자 중복 발급 방지”와 “재고 소진 시 정확하게 차감”이라는 데이터 정합성 요건을 충족해야 함
    4. **비즈니스 영향도**
        - 쿠폰 발급 장애는 프로모션 전체를 무효화시키거나 사용자 불만으로 이어져 매출 및 브랜드 이미지에 악영향을 미칠 수 있으므로, 장애 대응 역량을 확인하기에 적합함

목적:
- **데이터 정합성 유지 여부 검증**
    - 대량 동시 요청 환경에서 Redis 기반 재고 차감 로직, Kafka 메시징, DB 저장이 올바르게 작동하여 “재고 초과 발급”이나 “중복 발급”이 발생하지 않는지 확인
- **성능·안정성 평가**
    - 짧은 시간에 1만~5만 명 이상의 사용자 요청을 소화할 때, API 응답 지연(latency)과 처리량(throughput)이 허용 범위 내에 있는지 측정
    - p95, p99 등 고지연 구간을 모니터링하여 장애 발생 가능성을 사전에 식별
- **장애 상황 대응 시나리오 검증**
    - Redis 연결 지연, Kafka 컨슈머 지연, DB 커넥션 풀 고갈 등 실제 운영 중 발생할 수 있는 장애 케이스를 시뮬레이션하여, 애플리케이션이 예외를 안전하게 처리하고 Failover 또는 재시도를 통해 복구할 수 있는지 점검

시나리오:
1. **테스트 환경 준비**
    - Redis: 쿠폰 재고(예: `coupon:%d:inventory` 리스트)에 사전 발급 가능한 수량(예: 25000개)을 `LPUSH`로 적재
    - Redis 세트(`coupon:%d:issued_users`) 초기화
2. **시나리오 설정**
   - **테스트 VU 수**: 300~10000 VU
   - 각 VU는 `userIdBase = 1`에서 랜덤으로 1~50,001 범위의 userId를 사용
    - **부하 기간**: 총 2분(120초)
    - **요청 분산**:
        - `ramping-arrival-rate` 방식을 사용하여 초당 요청 비율이 단계적으로 변경되도록 설정
        - **startRate**: 300 RPS (초기)
        - **stages**:
            1. 30초 동안 초당 1000건 요청
            2. 다음 30초 동안 초당 2500건 요청
            3. 다음 30초 동안 초당 5000건 요청
            4. 마지막 30초 동안 초당 100건 요청
        - **preAllocatedVUs**: 300
        - **maxVUs**: 10,000
    - **k6 스크립트 주요 로직**:
      ```js
      import http from 'k6/http';
      import { check } from 'k6';

      export const options = {
          scenarios: {
              coupon_issuance_high: {
                  executor: 'ramping-arrival-rate',
                  startRate: 300,
                  timeUnit: '1s',
                  preAllocatedVUs: 300,
                  maxVUs: 10000,
                  stages: [
                      { target: 1000, duration: '30s' },
                      { target: 2500, duration: '30s' },
                      { target: 5000, duration: '30s' },
                      { target: 100, duration: '30s' },
                  ],
                  exec: 'default',
              },
          },
          summaryTrendStats: ['min', 'avg', 'med', 'max', 'p(50)', 'p(90)', 'p(95)', 'p(99)'],
      };

      const baseUrl = 'http://localhost:8080';

      export default function () {
          const userIdBase = 1;
          const userId = userIdBase + Math.floor(Math.random() * 50000);
          const couponId = 8;

          const res = http.post(
              `${baseUrl}/coupons/${userId}/issue-async?couponId=${couponId}`,
              null,
              { tags: { name: 'CouponIssue' } },
              { timeout: '120s' }
          );

          check(res, {
              'status is 2xx': (r) => r.status >= 200 && r.status < 300,
          });
      }
      ```
3. **실행 및 모니터링**
    - **k6 실행**:
      ```bash
      k6 run --out influxdb=http://localhost:8086/k6 k6-coupon-issue.js
      ```  
    - **Grafana 대시보드 구성**:
        1. 총 요청 수 (`http_reqs{group="CouponIssue"}`)
        2. 오류 비율 (`http_req_failed{group="CouponIssue"}`)
        3. 응답 지연 분포 (`http_req_duration{group="CouponIssue"}` → p50, p95, p99)
        4. Redis 재고 차감 실패 비율(애플리케이션 로그를 메트릭으로 수집하거나, Redis `LPOP` 실패 카운트)
        5. DB 중복 예외(DataIntegrityViolationException) 발생 여부
        6. 서버 리소스(CPU, 메모리), Redis 사용량, DB 커넥션 풀 상태, Kafka consumer lag 등
4. **결과 수집 및 분석**
    - **성공 처리율**: 총 요청 대비 HTTP 2xx 응답 비율
    - **주요 지연 구간**: p95, p99
    - **오류 유형 및 빈도**:
        - **타임아웃**
        - **중복 발급 예외** (`IllegalStateException: 이미 발급`)
        - **재고 없음 예외** (`IllegalStateException: 재고 없음`)
        - **DB 중복 키 예외**