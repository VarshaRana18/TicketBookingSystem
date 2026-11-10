import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';
import exec from 'k6/execution';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const SHOW_ID = Number(__ENV.SHOW_ID);
const START = Number(__ENV.START || 0);   // offset so reruns use fresh seats

const bookingsCreated = new Counter('bookings_created');
const seatTaken409 = new Counter('responses_409_seat_taken');
const busy503 = new Counter('responses_503_busy');
const otherErrors = new Counter('responses_other_errors');

export const options = {
    scenarios: {
        book: {
            executor: 'ramping-arrival-rate',
            startRate: 10,
            timeUnit: '1s',
            preAllocatedVUs: 100,
            maxVUs: 500,
            stages: [
                { target: 50, duration: '20s' },
                { target: 100, duration: '30s' },
                { target: 200, duration: '30s' },
            ],
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<1000'],
        responses_other_errors: ['count==0'],
    },
};

export function setup() {
    const res = http.post(
        `${BASE}/api/auth/login`,
        JSON.stringify({ email: __ENV.EMAIL, password: __ENV.PASSWORD }),
        { headers: { 'Content-Type': 'application/json' } }
    );
    check(res, { 'login ok': (r) => r.status === 200 });
    return { token: res.json('token') };
}

export default function (data) {
    const n = START + exec.scenario.iterationInTest;     // unique number per request
    const row = String.fromCharCode(65 + (n % 26));       // A..Z
    const number = Math.floor(n / 26) + 1;                // 1..1000

    const res = http.post(
        `${BASE}/api/bookings/initiate`,
        JSON.stringify({ showId: SHOW_ID, seats: [{ seatRow: row, seatNumber: number }] }),
        { headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${data.token}` } }
    );

    if (res.status === 200 || res.status === 201) bookingsCreated.add(1);
    else if (res.status === 409) seatTaken409.add(1);
    else if (res.status === 503) busy503.add(1);
    else otherErrors.add(1);
}