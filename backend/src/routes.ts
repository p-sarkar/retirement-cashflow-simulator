import { type Context, Router } from "@oak/oak";
import { downstreamError } from "./middleware/errorHandler.ts";
import type {
  BreakdownRequest,
  YearBreakdown,
  MarketPreset,
  MonteCarloRequest,
  MonteCarloResult,
  MonteCarloRunBreakdownRequest,
  MonteCarloRunDetail,
  MonteCarloRunRequest,
  SavedSimulation,
  SavedSimulationSummary,
  SaveSimulationRequest,
  SimulationConfig,
  SimulationResult,
  SpendingStrategyInfo,
} from "./types.ts";

const API_SERVER_URL = "http://localhost:8090";

const router = new Router();

router.get("/", (ctx) => {
  ctx.response.body = "Hello from Deno BFF!";
});

router.get("/health", (ctx) => {
  ctx.response.body = { status: "ok" };
});

/**
 * Forward a request to the Kotlin API server. Status codes follow contracts/api-contract.md:
 * 400 for malformed JSON, API errors (e.g. 422 validation) passed through with their status
 * (see downstreamError), 503 if the API server is unreachable. Anything thrown is turned into
 * a JSON 500 by the errorHandler middleware.
 *
 * The type parameters document the contract for each route; the body is forwarded as is.
 */
async function proxy<Req, Res extends object>(ctx: Context, method: "GET" | "POST", path: string, successStatus = 200) {
  let body: Req | undefined;
  if (method === "POST") {
    try {
      body = await ctx.request.body.json();
    } catch {
      ctx.response.status = 400;
      ctx.response.body = { error: "Request body must be valid JSON" };
      return;
    }
  }

  let response: Response;
  try {
    response = await fetch(`${API_SERVER_URL}${path}`, {
      method,
      headers: method === "POST" ? { "Content-Type": "application/json" } : undefined,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch (error) {
    console.error(`API server unreachable for ${path}:`, error);
    ctx.response.status = 503;
    ctx.response.body = { error: "Simulation API server is unavailable" };
    return;
  }

  if (!response.ok) {
    const { status, body: errorBody, contentType } = await downstreamError(response);
    ctx.response.status = status;
    if (contentType) ctx.response.headers.set("Content-Type", contentType);
    ctx.response.body = errorBody;
    return;
  }
  const data: Res = await response.json();
  ctx.response.status = successStatus;
  ctx.response.body = data;
}

// Interactive simulation (US002-1)
router.post("/api/simulate", (ctx) => proxy<SimulationConfig, SimulationResult>(ctx, "POST", "/api/simulate"));

router.post(
  "/api/simulate/breakdown",
  (ctx) => proxy<BreakdownRequest, YearBreakdown>(ctx, "POST", "/api/simulate/breakdown"),
);

router.get("/api/market-presets", (ctx) => proxy<never, MarketPreset[]>(ctx, "GET", "/api/market-presets"));

// Spending strategy catalog (spec 006)
router.get(
  "/api/spending-strategies",
  (ctx) => proxy<never, SpendingStrategyInfo[]>(ctx, "GET", "/api/spending-strategies"),
);

// Saved simulations (US002-2)
router.get("/api/simulations", (ctx) => proxy<never, SavedSimulationSummary[]>(ctx, "GET", "/api/simulations"));

router.post(
  "/api/simulations",
  (ctx) => proxy<SaveSimulationRequest, SavedSimulationSummary>(ctx, "POST", "/api/simulations", 201),
);

router.get(
  "/api/simulations/:id",
  (ctx) => proxy<never, SavedSimulation>(ctx, "GET", `/api/simulations/${encodeURIComponent(ctx.params.id)}`),
);

// Monte Carlo (US002-3) and single-run details (US002-4)
router.post(
  "/api/simulate/monte-carlo",
  (ctx) => proxy<MonteCarloRequest, MonteCarloResult>(ctx, "POST", "/api/simulate/monte-carlo"),
);

router.post(
  "/api/simulate/monte-carlo/run",
  (ctx) => proxy<MonteCarloRunRequest, MonteCarloRunDetail>(ctx, "POST", "/api/simulate/monte-carlo/run"),
);

// Computation breakdown for one year of one run (spec 004 US004-6)
router.post(
  "/api/simulate/monte-carlo/run/breakdown",
  (ctx) =>
    proxy<MonteCarloRunBreakdownRequest, YearBreakdown>(ctx, "POST", "/api/simulate/monte-carlo/run/breakdown"),
);

export default router;
