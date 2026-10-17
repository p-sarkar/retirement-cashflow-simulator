import { assertEquals } from "@std/assert";
import { API_SERVER_URL, createBff } from "./helpers.ts";

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
    assertEquals(bff.forwarded, [{ method: "POST", url: `${API_SERVER_URL}/api/simulate`, body: configWithExpenses }]);
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
    assertEquals(bff.forwarded, [{ method: "POST", url: `${API_SERVER_URL}/api/simulate/breakdown`, body: request }]);
  } finally {
    bff.restore();
  }
});

for (const path of ["/api/simulate", "/api/simulate/breakdown", "/api/simulate/monte-carlo/run/breakdown"]) {
  Deno.test(`POST ${path} returns 503 when the API server is unreachable`, async () => {
    const bff = createBff("unreachable");
    try {
      const res = await bff.post(path, configWithExpenses);

      assertEquals(res.status, 503);
      assertEquals(await res.json(), { error: "Simulation API server is unavailable" });
    } finally {
      bff.restore();
    }
  });

  Deno.test(`POST ${path} returns 400 for malformed JSON without calling the API server`, async () => {
    const bff = createBff({ status: 200, body: {} });
    try {
      const res = await bff.post(path, "{not json", true);

      assertEquals(res.status, 400);
      assertEquals(await res.json(), { error: "Request body must be valid JSON" });
      assertEquals(bff.forwarded, []);
    } finally {
      bff.restore();
    }
  });
}
