import { assertEquals } from "@std/assert";
import { API_SERVER_URL, createBff } from "./helpers.ts";

const config = { name: "BFF test", currentYear: 2025, currentAge: 60 };
const summary = {
  id: "6f1c2a4e-0000-4000-8000-000000000001",
  name: "Retire at 65",
  createdAt: "2026-10-15T10:49:03-07:00",
  isSuccess: true,
  failureYear: null,
  finalTotalBalance: 1234567,
  endAge: 85,
};

// --- Saved simulations (US2) ---

Deno.test("GET /api/simulations lists saved simulations", async () => {
  const bff = createBff({ status: 200, body: [summary] });
  try {
    const res = await bff.get("/api/simulations");

    assertEquals(res.status, 200);
    assertEquals(await res.json(), [summary]);
    assertEquals(bff.forwarded, [{ method: "GET", url: `${API_SERVER_URL}/api/simulations`, body: undefined }]);
  } finally {
    bff.restore();
  }
});

Deno.test("POST /api/simulations saves and returns 201 with the summary", async () => {
  const bff = createBff({ status: 201, body: summary });
  try {
    const request = { name: "Retire at 65", result: { config, yearlyResults: [] } };
    const res = await bff.post("/api/simulations", request);

    assertEquals(res.status, 201);
    assertEquals(await res.json(), summary);
    assertEquals(bff.forwarded, [{ method: "POST", url: `${API_SERVER_URL}/api/simulations`, body: request }]);
  } finally {
    bff.restore();
  }
});

Deno.test("GET /api/simulations/:id loads one saved simulation", async () => {
  const saved = { summary, result: { config } };
  const bff = createBff({ status: 200, body: saved });
  try {
    const res = await bff.get(`/api/simulations/${summary.id}`);

    assertEquals(res.status, 200);
    assertEquals(await res.json(), saved);
    assertEquals(bff.forwarded[0].url, `${API_SERVER_URL}/api/simulations/${summary.id}`);
  } finally {
    bff.restore();
  }
});

Deno.test("GET /api/simulations/:id passes a JSON 404 through", async () => {
  const notFound = { error: "Saved simulation x not found", statusCode: 404 };
  const bff = createBff({ status: 404, body: notFound });
  try {
    const res = await bff.get(`/api/simulations/${summary.id}`);

    assertEquals(res.status, 404);
    assertEquals(await res.json(), notFound);
  } finally {
    bff.restore();
  }
});

// --- Market presets, Monte Carlo (US3, US4) ---

Deno.test("GET /api/market-presets proxies the presets", async () => {
  const presets = [{ id: "crash-2008", name: "2008 Crash" }];
  const bff = createBff({ status: 200, body: presets });
  try {
    const res = await bff.get("/api/market-presets");

    assertEquals(await res.json(), presets);
    assertEquals(bff.forwarded[0].url, `${API_SERVER_URL}/api/market-presets`);
  } finally {
    bff.restore();
  }
});

for (const path of ["/api/simulate/monte-carlo", "/api/simulate/monte-carlo/run"]) {
  Deno.test(`POST ${path} forwards the request and returns the result`, async () => {
    const result = { seed: 42, paths: [] };
    const bff = createBff({ status: 200, body: result });
    try {
      const request = { config, seed: 42, runIndex: 3 };
      const res = await bff.post(path, request);

      assertEquals(res.status, 200);
      assertEquals(await res.json(), result);
      assertEquals(bff.forwarded, [{ method: "POST", url: `${API_SERVER_URL}${path}`, body: request }]);
    } finally {
      bff.restore();
    }
  });
}

// --- Error handling (T040) ---

Deno.test("a plain-text API error is wrapped as JSON with its status", async () => {
  const bff = createBff({ status: 400, body: "Age 99 not found in simulation results", contentType: "text/plain" });
  try {
    const res = await bff.post("/api/simulate/breakdown", { config, targetAge: 99 });

    assertEquals(res.status, 400);
    assertEquals(res.headers.get("Content-Type"), "application/json; charset=UTF-8");
    assertEquals(await res.json(), { error: "Age 99 not found in simulation results", statusCode: 400 });
  } finally {
    bff.restore();
  }
});

Deno.test("an empty API error body is described by its status", async () => {
  const bff = createBff({ status: 500, body: "", contentType: "text/plain" });
  try {
    const res = await bff.get("/api/simulations");

    assertEquals(res.status, 500);
    assertEquals(await res.json(), { error: "API server error (HTTP 500)", statusCode: 500 });
  } finally {
    bff.restore();
  }
});

Deno.test("an invalid JSON success body from the API becomes a JSON 500", async () => {
  const bff = createBff({ status: 200, body: "{truncated", contentType: "application/json" });
  try {
    const res = await bff.post("/api/simulate", config);

    assertEquals(res.status, 500);
    assertEquals(await res.json(), { error: "Internal Server Error", statusCode: 500 });
  } finally {
    bff.restore();
  }
});

Deno.test("an unknown route is a JSON 404", async () => {
  const bff = createBff({ status: 200, body: {} });
  try {
    const res = await bff.get("/api/nope");

    assertEquals(res.status, 404);
    assertEquals(await res.json(), { error: "No route for GET /api/nope", statusCode: 404 });
    assertEquals(bff.forwarded, []);
  } finally {
    bff.restore();
  }
});
