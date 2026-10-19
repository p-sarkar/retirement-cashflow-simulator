import { type Context, Router } from "@oak/oak";
import { downstreamError } from "./middleware/errorHandler.ts";
import type {
  BreakdownRequest,
  InputsFile,
  YearBreakdown,
  MarketPreset,
  MonteCarloRequest,
  MonteCarloResult,
  MonteCarloRunBreakdownRequest,
  MonteCarloRunDetail,
  MonteCarloRunRequest,
  RenameSimulationRequest,
  SavedSimulation,
  SavedSimulationSummary,
  SaveSimulationRequest,
  SimulationConfig,
  SimulationResult,
  UpdateSimulationRequest,
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
async function proxy<Req, Res extends object>(
  ctx: Context,
  method: "GET" | "POST" | "PUT" | "PATCH" | "DELETE",
  path: string,
  successStatus = 200,
  maxBodyBytes?: number, // request validation: refuse larger bodies with 413 (spec 005 FR005-016)
) {
  const hasBody = method === "POST" || method === "PUT" || method === "PATCH";
  let body: Req | undefined;
  if (hasBody) {
    try {
      const text = await ctx.request.body.text();
      if (maxBodyBytes !== undefined && new TextEncoder().encode(text).length > maxBodyBytes) {
        ctx.response.status = 413;
        ctx.response.body = { error: `Request body must be ${maxBodyBytes / (1024 * 1024)} MB or less`, statusCode: 413 };
        return;
      }
      body = JSON.parse(text);
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
      headers: hasBody ? { "Content-Type": "application/json" } : undefined,
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
  if (response.status === 204) {
    ctx.response.status = 204; // e.g. a delete: no body
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

// Update, rename and delete a saved simulation (spec 005 US005-2)
router.put(
  "/api/simulations/:id",
  (ctx) =>
    proxy<UpdateSimulationRequest, SavedSimulationSummary>(
      ctx,
      "PUT",
      `/api/simulations/${encodeURIComponent(ctx.params.id)}`,
    ),
);

router.patch(
  "/api/simulations/:id",
  (ctx) =>
    proxy<RenameSimulationRequest, SavedSimulationSummary>(
      ctx,
      "PATCH",
      `/api/simulations/${encodeURIComponent(ctx.params.id)}`,
    ),
);

router.delete(
  "/api/simulations/:id",
  (ctx) => proxy<never, never>(ctx, "DELETE", `/api/simulations/${encodeURIComponent(ctx.params.id)}`),
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

// Export and import a simulation's inputs as a file (spec 005 US005-3)
const MAX_IMPORT_BYTES = 1024 * 1024;

router.post(
  "/api/inputs/export",
  (ctx) => proxy<SimulationConfig, InputsFile>(ctx, "POST", "/api/inputs/export"),
);

router.post(
  "/api/inputs/import",
  (ctx) => proxy<InputsFile, SimulationConfig>(ctx, "POST", "/api/inputs/import", 200, MAX_IMPORT_BYTES),
);

export default router;
