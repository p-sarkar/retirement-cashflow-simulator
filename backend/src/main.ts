import { createApp } from "./app.ts";

const app = createApp();
const port = Number(Deno.env.get("PORT") ?? 8000);

console.log(`Server running on http://localhost:${port}`);
await app.listen({ port });
