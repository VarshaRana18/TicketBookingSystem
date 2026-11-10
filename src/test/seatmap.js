import http from 'k6/http';
import { check } from 'k6';

export const options = {
    scenarios: {
        ramp: {
            executor: 'ramping-arrival-rate',
            startRate: 100,
            timeUnit: '1s',
            preAllocatedVUs: 200,
            maxVUs: 1000,
            stages: [
                { target: 300, duration: '30s' },
                { target: 600, duration: '30s' },
                { target: 1000, duration: '30s' },
            ],
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<500'],
    },
};

export default function () {
    const res = http.get('http://localhost:8080/api/public/shows/6/seats');
    check(res, { 'status is 200': (r) => r.status === 200 });
}