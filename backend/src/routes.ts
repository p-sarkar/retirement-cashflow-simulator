import { type Context, Router } from "@oak/oak";
import type { BreakdownRequest, ComputationBreakdown, SimulationConfig, SimulationResult } from "./types.ts";

const API_SERVER_URL = "http://localhost:8090";

const router = new Router();

router.get("/", (ctx) => {
  ctx.response.body = "Hello from Deno BFF!";
});

router.get("/health", (ctx) => {
  ctx.response.body = { status: "ok" };
});

/**
 * Forward a JSON POST to the Kotlin API server. Status codes follow contracts/api-contract.md:
 * 400 for malformed JSON, API errors (e.g. 422 validation) passed through unchanged,
 * 503 if the API server is unreachable, 500 for anything else.
 */
async function proxyPost<Req, Res extends SimulationResult | ComputationBreakdown>(ctx: Context, path: string) {
  let body: Req;
  try {
    body = await ctx.request.body.json();
  } catch {
    ctx.response.status = 400;
    ctx.response.body = { error: "Request body must be valid JSON" };
    return;
  }

  let response: Response;
  try {
    response = await fetch(`${API_SERVER_URL}${path}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
  } catch (error) {
    console.error(`API server unreachable for ${path}:`, error);
    ctx.response.status = 503;
    ctx.response.body = { error: "Simulation API server is unavailable" };
    return;
  }

  try {
    if (!response.ok) {
      // Pass errors through unchanged (e.g. 422 validation errors as JSON)
      ctx.response.status = response.status;
      ctx.response.headers.set("Content-Type", response.headers.get("Content-Type") ?? "text/plain");
      ctx.response.body = await response.text();
      return;
    }
    const data: Res = await response.json();
    ctx.response.body = data;
  } catch (error) {
    console.error(`Error proxying ${path} to API server:`, error);
    ctx.response.status = 500;
    ctx.response.body = { error: "Internal Server Error" };
  }
}

// Proxy to Kotlin API Server
router.post("/api/simulate", (ctx) => proxyPost<SimulationConfig, SimulationResult>(ctx, "/api/simulate"));

// Proxy for computation breakdown
router.post(
  "/api/simulate/breakdown",
  (ctx) => proxyPost<BreakdownRequest, ComputationBreakdown>(ctx, "/api/simulate/breakdown"),
);

export default router;
