import http from 'k6/http';
import { check } from 'k6';

export const options = {
    vus: 1,
    duration: '10s',
};

export default function () {
    const res = http.get('http://localhost:8080/products/popular');
    console.log(`응답 상태: ${res.status}, 응답 본문: ${res.body}`);
    check(res, {
        '✅ 상태 코드 200': (r) => r.status === 200,
        '✅ 응답에 productId 포함': (r) => r.body.includes('productId'),
    });
}