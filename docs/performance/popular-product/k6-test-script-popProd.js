import http from 'k6/http';
import {check} from 'k6';

export const options = {
    stages: [
        {duration: '30s', target: 100},  // 점진 증가
        {duration: '1m', target: 300},   // 고부하 유지
        {duration: '30s', target: 0},    // 부하 제거
    ],
    thresholds: {
        http_req_duration: [
            // 'p(99)<99999',  // 실패 여부는 중요치 않으므로 넉넉히 설정
            // 'p(95)<99999',
            // 'p(90)<99999',
            // 'p(50)<99999',
        ],
    },
    summaryTrendStats: ['min', 'avg', 'med', 'max', 'p(50)', 'p(90)', 'p(95)', 'p(99)'],
};

export default function () {
    const res = http.get('http://localhost:8080/products/popular');

    check(res, {
        'status is 200': (r) => r.status === 200,
        '응답에 productId 포함': (r) => r.json()[0]?.productId !== undefined,
    });
}