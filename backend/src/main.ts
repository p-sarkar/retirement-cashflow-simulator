import { Application } from "@oak/oak";
import router from "./routes.ts";

const app = new Application();
const port = Number(Deno.env.get("PORT") ?? 8000);

// Allow the Vite dev server (another origin) to call the BFF; preflight requests end here
app.use(async (ctx, next) => {
  ctx.response.headers.set("Access-Control-Allow-Origin", "*");
  ctx.response.headers.set("Access-Control-Allow-Methods", "GET,HEAD,POST,OPTIONS");
  ctx.response.headers.set("Access-Control-Allow-Headers", ctx.request.headers.get("Access-Control-Request-Headers") ?? "Content-Type");
  if (ctx.request.method === "OPTIONS") {
    ctx.response.status = 204;
    return;
  }
  await next();
});
app.use(router.routes());
app.use(router.allowedMethods());

console.log(`Server running on http://localhost:${port}`);
await app.listen({ port });
