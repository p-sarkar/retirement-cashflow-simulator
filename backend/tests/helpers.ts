import { createApp } from "../src/app.ts";

export const API_SERVER_URL = "http://localhost:8090";

export interface Upstream {
  status: number;
  body: unknown;
  contentType?: string;
}

export interface Forwarded {
  method: string;
  url: string;
  body: unknown;
}

/**
 * Build the BFF app with fetch() to the Kotlin API stubbed.
 * Requests are dispatched with app.handle() (no network listener needed).
 */
export function createBff(upstream: Upstream | "unreachable") {
  const realFetch = globalThis.fetch;
  const forwarded: Forwarded[] = [];

  globalThis.fetch = (input: string | URL | Request, init?: RequestInit) => {
    const url = input instanceof Request ? input.url : input.toString();
    if (!url.startsWith(API_SERVER_URL)) return realFetch(input, init);
    forwarded.push({ method: init?.method ?? "GET", url, body: init?.body ? JSON.parse(String(init.body)) : undefined });
    if (upstream === "unreachable") {
      return Promise.reject(new TypeError("error sending request: Connection refused (os error 61)"));
    }
    const body = typeof upstream.body === "string" ? upstream.body : JSON.stringify(upstream.body);
    return Promise.resolve(
      new Response(body, {
        status: upstream.status,
        headers: { "Content-Type": upstream.contentType ?? "application/json" },
      }),
    );
  };

  const app = createApp();

  const handle = async (request: Request): Promise<Response> => {
    const res = await app.handle(request);
    if (!res) throw new Error(`No response for ${request.method} ${request.url}`);
    return res;
  };

  return {
    post: (path: string, body: unknown, raw = false): Promise<Response> =>
      handle(
        new Request(`http://bff.test${path}`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: raw ? String(body) : JSON.stringify(body),
        }),
      ),
    get: (path: string): Promise<Response> => handle(new Request(`http://bff.test${path}`)),
    forwarded,
    restore: () => {
      globalThis.fetch = realFetch;
    },
  };
}
