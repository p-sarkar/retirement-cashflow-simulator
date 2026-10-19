import { Application } from "@oak/oak";
import { errorHandler } from "./middleware/errorHandler.ts";
import router from "./routes.ts";

/** The BFF application: CORS, error handling and the routes. Tests drive it with app.handle(). */
export function createApp(): Application {
  const app = new Application();

  // Allow the Vite dev server (another origin) to call the BFF; preflight requests end here
  app.use(async (ctx, next) => {
    ctx.response.headers.set("Access-Control-Allow-Origin", "*");
    ctx.response.headers.set("Access-Control-Allow-Methods", "GET,HEAD,POST,PUT,PATCH,DELETE,OPTIONS");
    ctx.response.headers.set("Access-Control-Allow-Headers", ctx.request.headers.get("Access-Control-Request-Headers") ?? "Content-Type");
    if (ctx.request.method === "OPTIONS") {
      ctx.response.status = 204;
      return;
    }
    await next();
  });
  app.use(errorHandler);
  app.use(router.routes());
  app.use(router.allowedMethods());

  return app;
}
