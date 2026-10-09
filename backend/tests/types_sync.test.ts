import { assertEquals } from "@std/assert";

// Constitution III: BFF and frontend contract types must stay in sync.
// Compares declarations only (comments and blank lines are ignored).
const declarations = (source: string) =>
  source
    .split("\n")
    .map((line) => line.replace(/\/\/.*$/, "").trimEnd())
    .filter((line) => line.trim() !== "");

Deno.test("backend/src/types.ts matches frontend/src/types/simulation.ts", async () => {
  const backend = await Deno.readTextFile(new URL("../src/types.ts", import.meta.url));
  const frontend = await Deno.readTextFile(new URL("../../frontend/src/types/simulation.ts", import.meta.url));

  assertEquals(declarations(backend), declarations(frontend));
});
