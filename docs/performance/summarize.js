const fs = require('fs');
const path = require('path');

const INPUT_PATH = path.resolve(__dirname, './k6-result.json');
const OUTPUT_PATH = path.resolve(__dirname, './k6-summary.md');

// JSON 읽기
const raw = fs.readFileSync(INPUT_PATH, 'utf-8');
const data = JSON.parse(raw);

// 값 추출
const metrics = data.metrics;
const duration = metrics?.http_req_duration || {};
const count = metrics?.http_reqs?.count;
const failRate = metrics?.http_req_failed?.value;

// 디버깅 출력
console.log("✅ count:", count);
console.log("✅ duration keys:", Object.keys(duration));

// 안전한 포맷터
const safe = (val) => (typeof val === 'number' ? val.toFixed(2) : 'N/A');

const md = `
# k6 성능 테스트 요약

- 📦 총 요청 수: **${count ?? 'N/A'}**
- ❌ 실패율: **${failRate !== undefined ? (failRate * 100).toFixed(2) : 'N/A'}%**

## ⏱️ 응답 시간 (http_req_duration)

| 구간 | 시간 (ms) |
|------|-----------|
| 평균 | ${safe(duration.avg)} |
| 최소 | ${safe(duration.min)} |
| 최대 | ${safe(duration.max)} |
| p50  | ${safe(duration['p(50)'])} |
| p90  | ${safe(duration['p(90)'])} |
| p95  | ${safe(duration['p(95)'])} |
| p99  | ${safe(duration['p(99)'])} |

> SLA 기준: **p99 < 300ms** 권장
`;

fs.writeFileSync(OUTPUT_PATH, md.trim());
console.log(`📄 요약 리포트 생성 완료: ${OUTPUT_PATH}`);