import { Application } from "https://deno.land/x/oak@v14.0.0/mod.ts";
import { assertEquals } from "@std/assert";
import router from "../src/routes.ts";

const API_SERVER_URL = "http://localhost:8090";

interface Upstream {
  status: number;
  body: unknown;
  contentType?: string;
}

/**
 * Build the BFF app with fetch() to the Kotlin API stubbed.
 * Requests are dispatched with app.handle() (no network listener needed).
 */
function createBff(upstream: Upstream) {
  const realFetch = globalThis.fetch;
  const forwarded: { url: string; body: unknown }[] = [];

  globalThis.fetch = (input: string | URL | Request, init?: RequestInit) => {
    const url = input instanceof Request ? input.url : input.toString();
    if (!url.startsWith(API_SERVER_URL)) return realFetch(input, init);
    forwarded.push({ url, body: JSON.parse(String(init?.body)) });
    return Promise.resolve(
      new Response(JSON.stringify(upstream.body), {
        status: upstream.status,
        headers: { "Content-Type": upstream.contentType ?? "application/json" },
      }),
    );
  };

  const app = new Application();
  app.use(router.routes());
  app.use(router.allowedMethods());

  return {
    post: async (path: string, body: unknown): Promise<Response> => {
      const res = await app.handle(
        new Request(`http://bff.test${path}`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(body),
        }),
      );
      if (!res) throw new Error(`No response for ${path}`);
      return res;
    },
    forwarded,
    restore: () => {
      globalThis.fetch = realFetch;
    },
  };
}

const configWithExpenses = {
  name: "BFF test",
  currentYear: 2025,
  currentAge: 60,
  oneTimeExpenses: [
    { type: "CASH", id: "c", name: "Roof", amount: 20000, yearOrAge: { type: "AGE", age: 65 } },
    {
      type: "LOAN",
      id: "l",
      name: "Car Loan",
      principal: 40000,
      aprPercent: 5,
      termYears: 5,
      startYearOrAge: { type: "YEAR", year: 2027 },
    },
  ],
};

Deno.test("POST /api/simulate forwards oneTimeExpenses and returns results unchanged", async () => {
  const result = {
    yearlyResults: [{
      year: 2027,
      cashFlow: { oneTimeExpenses: 9644.92 },
      oneTimeExpensesBreakdown: [{ name: "Car Loan", amount: 9644.92, type: "LOAN_PAYMENT" }],
    }],
  };
  const bff = createBff({ status: 200, body: result });
  try {
    const res = await bff.post("/api/simulate", configWithExpenses);

    assertEquals(res.status, 200);
    assertEquals(await res.json(), result);
    assertEquals(bff.forwarded, [{ url: `${API_SERVER_URL}/api/simulate`, body: configWithExpenses }]);
  } finally {
    bff.restore();
  }
});

Deno.test("POST /api/simulate passes 422 validation errors through as JSON", async () => {
  const validation = {
    error: "Validation failed",
    statusCode: 422,
    validationErrors: [{ field: "oneTimeExpenses[1].termYears", message: "Term must be greater than zero" }],
  };
  const bff = createBff({ status: 422, body: validation });
  try {
    const res = await bff.post("/api/simulate", configWithExpenses);

    assertEquals(res.status, 422);
    assertEquals(res.headers.get("Content-Type"), "application/json");
    assertEquals(await res.json(), validation);
  } finally {
    bff.restore();
  }
});

Deno.test("POST /api/simulate/breakdown passes 422 validation errors through as JSON", async () => {
  const validation = {
    error: "Validation failed",
    statusCode: 422,
    validationErrors: [{ field: "oneTimeExpenses[0].amount", message: "Amount must be greater than zero" }],
  };
  const bff = createBff({ status: 422, body: validation });
  try {
    const request = { config: configWithExpenses, targetAge: 63 };
    const res = await bff.post("/api/simulate/breakdown", request);

    assertEquals(res.status, 422);
    assertEquals(res.headers.get("Content-Type"), "application/json");
    assertEquals(await res.json(), validation);
    assertEquals(bff.forwarded, [{ url: `${API_SERVER_URL}/api/simulate/breakdown`, body: request }]);
  } finally {
    bff.restore();
  }
});
