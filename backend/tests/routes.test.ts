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
  updatedAt: null,
  version: "1.1.29",
  isCurrentVersion: true,
};

// --- Saved simulations (US002-2) ---

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

// --- Update, rename, delete (spec 005 US005-2, R005-8) ---

Deno.test("PUT /api/simulations/:id forwards the update and returns the summary", async () => {
  const updated = { ...summary, updatedAt: "2026-10-18T10:00:00-04:00" };
  const bff = createBff({ status: 200, body: updated });
  try {
    const request = { result: { config, yearlyResults: [] } };
    const res = await bff.send("PUT", `/api/simulations/${summary.id}`, request);

    assertEquals(res.status, 200);
    assertEquals(await res.json(), updated);
    assertEquals(bff.forwarded, [{ method: "PUT", url: `${API_SERVER_URL}/api/simulations/${summary.id}`, body: request }]);
  } finally {
    bff.restore();
  }
});

Deno.test("PATCH /api/simulations/:id forwards the rename and passes a 422 through", async () => {
  const invalid = { error: "Validation failed", statusCode: 422, validationErrors: [{ field: "name", message: "Name is required" }] };
  const bff = createBff({ status: 422, body: invalid });
  try {
    const res = await bff.send("PATCH", `/api/simulations/${summary.id}`, { name: " " });

    assertEquals(res.status, 422);
    assertEquals(await res.json(), invalid);
    assertEquals(bff.forwarded, [{ method: "PATCH", url: `${API_SERVER_URL}/api/simulations/${summary.id}`, body: { name: " " } }]);
  } finally {
    bff.restore();
  }
});

Deno.test("DELETE /api/simulations/:id returns 204 with no body", async () => {
  const bff = createBff({ status: 204, body: null });
  try {
    const res = await bff.send("DELETE", `/api/simulations/${summary.id}`);

    assertEquals(res.status, 204);
    assertEquals(await res.text(), "");
    assertEquals(bff.forwarded, [{ method: "DELETE", url: `${API_SERVER_URL}/api/simulations/${summary.id}`, body: undefined }]);
  } finally {
    bff.restore();
  }
});

Deno.test("DELETE /api/simulations/:id passes a JSON 404 through", async () => {
  const notFound = { error: "Saved simulation x not found", statusCode: 404 };
  const bff = createBff({ status: 404, body: notFound });
  try {
    const res = await bff.send("DELETE", `/api/simulations/${summary.id}`);

    assertEquals(res.status, 404);
    assertEquals(await res.json(), notFound);
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

// --- Market presets, Monte Carlo (US002-3, US002-4) ---

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

for (const path of ["/api/simulate/monte-carlo", "/api/simulate/monte-carlo/run", "/api/simulate/monte-carlo/run/breakdown"]) {
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

// --- Error handling (T002-040) ---

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

// --- Export and import inputs (spec 005 US005-3, R005-8) ---

Deno.test("POST /api/inputs/export forwards the inputs and returns the file", async () => {
  const file = { format: "retirement-simulator-inputs", formatVersion: 1, appVersion: "1.1.29", config };
  const bff = createBff({ status: 200, body: file });
  try {
    const res = await bff.post("/api/inputs/export", config);

    assertEquals(res.status, 200);
    assertEquals(await res.json(), file);
    assertEquals(bff.forwarded, [{ method: "POST", url: `${API_SERVER_URL}/api/inputs/export`, body: config }]);
  } finally {
    bff.restore();
  }
});

Deno.test("POST /api/inputs/import forwards the file and returns its inputs", async () => {
  const file = { format: "retirement-simulator-inputs", formatVersion: 1, appVersion: "1.1.29", config };
  const bff = createBff({ status: 200, body: config });
  try {
    const res = await bff.post("/api/inputs/import", file);

    assertEquals(res.status, 200);
    assertEquals(await res.json(), config);
    assertEquals(bff.forwarded, [{ method: "POST", url: `${API_SERVER_URL}/api/inputs/import`, body: file }]);
  } finally {
    bff.restore();
  }
});

Deno.test("POST /api/inputs/import refuses a file over 1 MB without calling the API server", async () => {
  const bff = createBff({ status: 200, body: config });
  try {
    const tooLarge = JSON.stringify({ format: "retirement-simulator-inputs", padding: "x".repeat(1024 * 1024) });
    const res = await bff.post("/api/inputs/import", tooLarge, true);

    assertEquals(res.status, 413);
    assertEquals((await res.json()).statusCode, 413);
    assertEquals(bff.forwarded, []);
  } finally {
    bff.restore();
  }
});

Deno.test("POST /api/simulations has no new size limit", async () => {
  const bff = createBff({ status: 201, body: summary });
  try {
    const large = { name: "Large", result: { config, padding: "x".repeat(1024 * 1024) } };
    const res = await bff.post("/api/simulations", large);

    assertEquals(res.status, 201);
    assertEquals(bff.forwarded.length, 1);
  } finally {
    bff.restore();
  }
});

// --- CORS: the browser's preflight must allow the methods the frontend sends (spec 005 US005-2) ---

Deno.test("a CORS preflight allows PUT, PATCH and DELETE", async () => {
  const bff = createBff({ status: 200, body: {} });
  try {
    for (const method of ["PUT", "PATCH", "DELETE"]) {
      const res = await bff.handle(
        new Request(`http://bff.test/api/simulations/${summary.id}`, {
          method: "OPTIONS",
          headers: { Origin: "http://localhost:5173", "Access-Control-Request-Method": method },
        }),
      );
      const allowed = (res.headers.get("Access-Control-Allow-Methods") ?? "").split(",");
      assertEquals(allowed.includes(method), true, `${method} allowed: ${allowed}`);
    }
    assertEquals(bff.forwarded, []);
  } finally {
    bff.restore();
  }
});
