// k6 smoke test: a light end-to-end flow through the API gateway.
// Run:  k6 run load-tests/k6/smoke.js
// Env:  BASE_URL (default http://localhost:8080), EMAIL, PASSWORD
import http from "k6/http";
import { check, sleep } from "k6";

const BASE = __ENV.BASE_URL || "http://localhost:8080";
const EMAIL = __ENV.EMAIL || `k6_${Date.now()}@example.com`;
const PASSWORD = __ENV.PASSWORD || "S3curePass!";

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<800"],
  },
};

export default function () {
  // Register (ignore 409 if the user already exists)
  let res = http.post(
    `${BASE}/api/v1/auth/register`,
    JSON.stringify({ email: EMAIL, password: PASSWORD, displayName: "k6" }),
    { headers: { "Content-Type": "application/json" } }
  );
  check(res, { "register 201 or 409": (r) => r.status === 201 || r.status === 409 });

  // Login
  res = http.post(
    `${BASE}/api/v1/auth/login`,
    JSON.stringify({ email: EMAIL, password: PASSWORD }),
    { headers: { "Content-Type": "application/json" } }
  );
  check(res, { "login 200": (r) => r.status === 200 });
  const token = res.json("accessToken");
  const auth = { headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" } };

  // Browse the catalog (public)
  res = http.get(`${BASE}/api/v1/products/search?q=`);
  check(res, { "search 200": (r) => r.status === 200 });

  // View the cart (authenticated)
  res = http.get(`${BASE}/api/v1/cart`, auth);
  check(res, { "cart 200": (r) => r.status === 200 });

  sleep(1);
}
