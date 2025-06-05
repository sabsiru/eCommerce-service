# k6 + Grafana + InfluxDB 기반 부하 테스트 시각화 가이드

## 1. 개요  
   이 문서는 Redis 기반 쿠폰 발급 API에 대해 k6로 부하 테스트를 수행하고, 결과를 Grafana 대시보드에서 시각화하는 전체 과정을 설명한다.

## 2. 아키텍처 구성  
  ```m
k6 → InfluxDB → Grafana
```

- k6: 테스트 실행 및 메트릭 수집
- InfluxDB: 시계열 DB로 메트릭 저장소 역할
- Grafana: InfluxDB를 데이터 소스로 하여 대시보드 구성

## 3. 환경 설정

### 3.1. Docker Compose 파일 예시  
다음 내용을 docker-compose.yml로 저장한다.
```yaml
version: '3'
services:
influxdb:
image: influxdb:1.8
ports:
  - "8086:8086"
environment:
  - INFLUXDB_DB=k6
  - INFLUXDB_ADMIN_USER=admin
  - INFLUXDB_ADMIN_PASSWORD=admin123

grafana:
image: grafana/grafana:9.6.2
ports:
  - "3000:3000"
environment:
  - GF_SECURITY_ADMIN_PASSWORD=admin123
depends_on:
  - influxdb
```

위 파일을 저장한 뒤, 터미널에서 다음 명령어를 실행한다.  
```bash
docker-compose up -d
```
InfluxDB 컨테이너 이름을 확인한 뒤, 터미널에서 접속한다.
```bash
docker exec -it <influxdb_container_name> influx
```
InfluxDB 콘솔에서 다음을 실행하여 데이터베이스를 생성한다.
```bash
CREATE DATABASE k6
```
## 4. k6 테스트 스크립트 작성

4.1. 파일명: k6-coupon-issue.js  
다음 내용을 k6-coupon-issue.js로 저장한다.
```javascript
import http from 'k6/http';  
import { check } from 'k6';

export const options = {  
scenarios: {  
coupon_issuance: {  
executor: 'constant-arrival-rate',  
rate: 1000,           // 1분 동안 1000건 요청  
timeUnit: '1m',       // 시간 단위: 1분  
duration: '1m',       // 테스트 총 길이: 1분  
preAllocatedVUs: 100, // 최소 VU 수  
maxVUs: 1000,         // 최대 VU 수  
},  
},  
};

const baseUrl = 'http://localhost:8080';

export default function () {  
const userIdBase = 41593;  
// 41593 ~ 42592 범위 내에서 랜덤 userId 선택  
const userId = userIdBase + Math.floor(Math.random() * 1000);  
const couponId = 1;

const res = http.post(`${baseUrl}/coupons/${userId}/issue-async?couponId=${couponId}`);

check(res, {  
'status is 200 or 201': (r) => r.status === 200 || r.status === 201,  
});  
}
```

## 5. k6 실행 및 InfluxDB 저장

터미널에서 다음 명령어를 실행하여 k6 테스트를 시작한다.  
```bash
k6 run --out influxdb=http://localhost:8086/k6 k6-coupon-issue.js
```
--out influxdb=http://localhost:8086/k6 옵션을 통해, 테스트 결과를 InfluxDB의 k6 데이터베이스에 저장한다.

## 6. Grafana 설정

### 6.1. Grafana 접속  
```
웹 브라우저에서 http://localhost:3000 접속  
기본 로그인 정보  
ID: admin  
PW: admin123
```

### 6.2. 데이터 소스 추가
```
왼쪽 사이드바 → Configuration → Data Sources 클릭  
InfluxDB 선택  
아래와 같이 설정  
• URL: http://influxdb:8086  
• Database: k6  
• User: admin  
• Password: admin123  
• Version: InfluxQL  
오른쪽 상단 Save & Test 클릭하여 연결 확인
```
## 7. 대시보드 생성 및 패널 추가

### 7.1. 새 대시보드 생성  
```
왼쪽 사이드바 → Create → Dashboard 클릭  
Add new panel 버튼 클릭
```
### 7.2. p99 응답시간 패널
```
Query 탭에서 설정  
• Data source: k6  
• Query (InfluxQL):  
SELECT percentile("value", 99)  
FROM "http_req_duration"  
WHERE $timeFilter  
GROUP BY time($__interval) fill(null)  
Visualization: Time series (선 그래프)  
Panel title: HTTP Request Duration (p99)  
오른쪽 상단 Apply 클릭
```
### 7.3. p95 응답시간 패널  
Add panel → Add an empty panel  
Query:  
SELECT percentile("value", 95)  
FROM "http_req_duration"  
WHERE $timeFilter  
GROUP BY time($__interval) fill(null)  
Visualization: Time series  
Panel title: HTTP Request Duration (p95)  
Apply

### 7.4. 평균 응답시간 패널  
Add panel → Add an empty panel  
Query:  
SELECT mean("value")  
FROM "http_req_duration"  
WHERE $timeFilter  
GROUP BY time($__interval) fill(null)  
Visualization: Time series  
Panel title: HTTP Request Duration (avg)  
Apply

### 7.5. TPS (Throughput) 패널  
Add panel → Add an empty panel  
Query:  
SELECT derivative(mean("value"), 1s) AS "throughput"  
FROM "http_reqs"  
WHERE $timeFilter  
GROUP BY time($__interval) fill(null)  
Visualization: Time series  
Panel title: RPS (Requests per Second)  
Apply

## 8. 대시보드 저장  
   대시보드 오른쪽 상단 Save dashboard 클릭  
   이름 예시: Coupon Issue Load Test  
   Save 클릭

## 9. 테스트 결과 확인  
   대시보드에서 실시간으로 p99, p95, 평균 응답시간, TPS 그래프를 확인  
   테스트 종료 후에도 지정한 시간 범위 내 데이터를 조회 가능

## 10. 요약
1) Docker로 InfluxDB와 Grafana 실행
2) k6 스크립트 작성 및 실행 (--out influxdb)
3) Grafana에서 InfluxDB 데이터 소스 설정
4) p99, p95, avg, TPS 패널 추가
5) 대시보드 저장 후 모니터링

위 과정을 따라가면 쿠폰 발급 API에 대한 부하 테스트 결과를 실시간으로 시각화할 수 있다. 필요 시 Grafana에서 알람, 경고 등을 추가 설정하여 운영 환경에서도 활용 가능하다.