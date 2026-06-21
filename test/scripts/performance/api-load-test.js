// ============================================================
// SmartReport API 性能测试 (k6)
// 覆盖: TC-PF-01 ~ TC-PF-04
//
// 安装: https://k6.io/docs/get-started/installation/
// 运行: k6 run test/scripts/performance/api-load-test.js
// ============================================================

import http from "k6/http";
import { check, sleep, group } from "k6";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const AI_URL = __ENV.AI_URL || "http://localhost:8000";

export const options = {
  stages: [
    { duration: "10s", target: 5 },   // 逐步上升到 5 并发
    { duration: "20s", target: 10 },  // 保持 10 并发
    { duration: "10s", target: 0 },   // 逐步下降
  ],
  thresholds: {
    // TC-PF-01: 搜索 API P95 < 200ms
    "http_req_duration{endpoint:search}": ["p(95)<200"],
    // TC-PF-02: KPI API P95 < 500ms
    "http_req_duration{endpoint:kpi}": ["p(95)<500"],
    // TC-PF-03: AI 引擎 P95 < 50ms
    "http_req_duration{endpoint:ai_health}": ["p(95)<50"],
  },
};

export default function () {
  // ── TC-PF-01: 搜索 API ──
  group("搜索 API", () => {
    const res = http.get(`${BASE_URL}/api/v1/search/companies?q=茅台&limit=8`, {
      tags: { endpoint: "search" },
    });
    check(res, {
      "搜索 HTTP 200": (r) => r.status === 200,
      "搜索响应含 data": (r) => r.json("code") === 0,
    });
  });
  sleep(0.5);

  // ── TC-PF-02: KPI API ──
  group("KPI API", () => {
    const res = http.get(`${BASE_URL}/api/v1/reports/600519/kpi`, {
      tags: { endpoint: "kpi" },
    });
    check(res, {
      "KPI HTTP 200": (r) => r.status === 200,
    });
  });
  sleep(0.5);

  // ── Benchmark API ──
  group("Benchmark API", () => {
    const res = http.get(`${BASE_URL}/api/v1/analysis/600519/benchmark`, {
      tags: { endpoint: "benchmark" },
    });
    check(res, {
      "Benchmark HTTP 200": (r) => r.status === 200,
    });
  });
  sleep(0.5);

  // ── TC-PF-03: AI 引擎健康检查 ──
  group("AI 引擎健康检查", () => {
    const res = http.get(`${AI_URL}/health`, {
      tags: { endpoint: "ai_health" },
    });
    check(res, {
      "AI 引擎 HTTP 200": (r) => r.status === 200,
    });
  });
  sleep(0.5);

  // ── TC-PF-04: RAG 检索 ──
  group("RAG 检索", () => {
    const payload = JSON.stringify({
      query: "毛利率",
      companyCode: "600519",
      topK: 5,
    });
    const res = http.post(`${AI_URL}/ai/v1/rag/search`, payload, {
      headers: { "Content-Type": "application/json" },
      tags: { endpoint: "rag" },
    });
    check(res, {
      "RAG HTTP 200": (r) => r.status === 200,
    });
  });
  sleep(0.5);
}

export function handleSummary(data) {
  console.log("");
  console.log("============================================");
  console.log(" SmartReport 性能测试结果");
  console.log("============================================");

  const metrics = data.metrics;
  if (metrics.http_req_duration) {
    const d = metrics.http_req_duration;
    console.log(`  平均响应时间: ${d.avg.toFixed(2)}ms`);
    console.log(`  P50: ${d.values.p50.toFixed(2)}ms`);
    console.log(`  P95: ${d.values.p95.toFixed(2)}ms`);
    console.log(`  P99: ${d.values["p(99)"]?.toFixed(2) || "N/A"}ms`);
  }
  console.log(`  失败率: ${(metrics.http_req_failed?.values?.rate * 100 || 0).toFixed(2)}%`);
  console.log("============================================");

  return {
    "test/scripts/performance/k6-summary.json": JSON.stringify(data, null, 2),
  };
}
