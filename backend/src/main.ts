import { Application, Router } from "https://deno.land/x/oak@v14.0.0/mod.ts";
import { oakCors } from "https://deno.land/x/cors@v1.2.2/mod.ts";

const router = new Router();
router.get("/", (ctx) => {
  ctx.response.body = "Hello from Deno BFF!";
});

// Health check
router.get("/health", (ctx) => {
  ctx.response.body = { status: "ok" };
});

const app = new Application();
const port = 8000;

app.use(oakCors()); // Enable CORS for all routes
app.use(router.routes());
app.use(router.allowedMethods());

console.log(`Server running on http://localhost:${port}`);
await app.listen({ port });