import { type Context, isHttpError, type Next } from "@oak/oak";
import type { ErrorResponse } from "../types.ts";

/**
 * Global error handling for the BFF (spec 002 T040). Every error the frontend gets is JSON:
 * - an exception thrown by a route or middleware becomes `{ error, statusCode }` (500 unless it is an HTTP error)
 * - an unknown route becomes a JSON 404
 * Errors from the API server are turned into client responses by `downstreamError`.
 */
export async function errorHandler(ctx: Context, next: Next) {
  try {
    await next();
    if (ctx.response.status === 404 && ctx.response.body === undefined) {
      jsonError(ctx, 404, `No route for ${ctx.request.method} ${ctx.request.url.pathname}`);
    }
  } catch (err) {
    const status = isHttpError(err) ? err.status : 500;
    console.error(`Unhandled error for ${ctx.request.method} ${ctx.request.url.pathname}:`, err);
    // Only HTTP errors meant for the client (e.g. 400s) show their message; anything else is internal
    const message = isHttpError(err) && err.expose ? err.message : "Internal Server Error";
    jsonError(ctx, status, message);
  }
}

export function jsonError(ctx: Context, status: number, error: string) {
  const body: ErrorResponse = { error, statusCode: status };
  ctx.response.status = status;
  ctx.response.type = "json";
  ctx.response.body = body;
}

/**
 * Turn an error response from the API server into the response for the client, keeping its status.
 * JSON error bodies (e.g. 422 validation errors, 404s) pass through unchanged, with their Content-Type; plain-text or empty
 * bodies are wrapped as `{ error, statusCode }` so the frontend always gets JSON.
 */
export async function downstreamError(
  response: Response,
): Promise<{ status: number; body: object; contentType?: string }> {
  const text = await response.text();
  const contentType = response.headers.get("Content-Type") ?? "";
  if (contentType.includes("application/json")) {
    try {
      return { status: response.status, body: JSON.parse(text), contentType };
    } catch {
      // Not valid JSON after all: wrap it below
    }
  }
  const error: ErrorResponse = {
    error: text.trim() || response.statusText || `API server error (HTTP ${response.status})`,
    statusCode: response.status,
  };
  return { status: response.status, body: error };
}
