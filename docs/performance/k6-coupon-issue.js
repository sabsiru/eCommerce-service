import http from 'k6/http';
import {check} from 'k6';

export const options = {
    scenarios: {
        coupon_issuance_high: {
            executor: 'ramping-arrival-rate',
            startRate: 300,
            timeUnit: '1s',
            preAllocatedVUs: 300,
            maxVUs: 10000,
            stages: [
                {target: 1000, duration: '30s'},
                {target: 2500, duration: '30s'},
                {target: 5000, duration: '30s'},
                {target: 100, duration: '30s'},
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
    const couponId = 10;

    const res = http.post(
        `${baseUrl}/coupons/${userId}/issue-async?couponId=${couponId}`,
        null,
        {tags: {name: 'CouponIssue'}},
    );

    check(
        res,
        {
            'status is 2xx': (r) => r.status >= 200 && r.status < 300,
        }
    )

}