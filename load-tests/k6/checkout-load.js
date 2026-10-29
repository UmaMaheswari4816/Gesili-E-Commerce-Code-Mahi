// k6 load test: sustained login + browse + cart under ramping load.
// Run:  k6 run load-tests/k6/checkout-load.js
import http from "k6/http";
import { check, sleep } from "k6";
import { Trend } from "k6/metrics";

const BASE = __ENV.BASE_URL || "http://localhost:8080";
const loginTrend = new Trend("login_duration", true);

export const options = {
  stages: [
    { duration: "30s", target: 20 },  // ramp up
    { duration: "1m", target: 20 },   // steady
    { duration: "30s", target: 50 },  // spike
    { duration: "30s", target: 0 },   // ramp down
  ],
  thresholds: {
    http_req_failed: ["rate<0.02"],
    http_req_duration: ["p(95)<1500"],
    login_duration: ["p(95)<1000"],
  },
};

export default function () {
  const email = `k6_${__VU}_${__ITER}@example.com`;
  const password = "S3curePass!";

  http.post(
    `${BASE}/api/v1/auth/register`,
    JSON.stringify({ email, password, displayName: "k6" }),
    { headers: { "Content-Type": "application/json" } }
  );

  const login = http.post(
    `${BASE}/api/v1/auth/login`,
    JSON.stringify({ email, password }),
    { headers: { "Content-Type": "application/json" } }
  );
  loginTrend.add(login.timings.duration);
  check(login, { "login ok": (r) => r.status === 200 });
  const token = login.json("accessToken");
  const auth = { headers: { Authorization: `Bearer ${token}` } };

  http.get(`${BASE}/api/v1/products/search?q=shirt`);
  http.get(`${BASE}/api/v1/cart`, auth);

  sleep(Math.random() * 2);
}
