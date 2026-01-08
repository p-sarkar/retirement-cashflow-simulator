import { Router } from "https://deno.land/x/oak@v14.0.0/mod.ts";

const API_SERVER_URL = "http://localhost:8090";

const router = new Router();

router.get("/", (ctx) => {
  ctx.response.body = "Hello from Deno BFF!";
});

router.get("/health", (ctx) => {
  ctx.response.body = { status: "ok" };
});

// Proxy to Kotlin API Server
router.post("/api/simulate", async (ctx) => {
  try {
    const body = await ctx.request.body.json();
    const response = await fetch(`${API_SERVER_URL}/api/simulate`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(body),
    });

    if (!response.ok) {
        ctx.response.status = response.status;
        ctx.response.body = await response.text();
        return;
    }

    const data = await response.json();
    ctx.response.body = data;
  } catch (error) {
    console.error("Error proxying to API server:", error);
    ctx.response.status = 500;
    ctx.response.body = { error: "Internal Server Error" };
  }
});

export default router;
