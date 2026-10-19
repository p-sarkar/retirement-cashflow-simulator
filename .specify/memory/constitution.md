# Retirement Cash Flow Simulator Constitution

## Core Principles

### I. Test-First Development (NON-NEGOTIABLE)
Feature implementation must begin with defining independent tests (Unit or Integration). Code should only be written to make failing tests pass. This is critical for ensuring the accuracy of financial calculations and simulation logic.

### II. Strict N-Tier Boundaries
Respect the architectural hierarchy to maintain separation of concerns:
1.  **Frontend (React)**: Presentation and user interaction only. No business logic. Communicates ONLY with the Deno Backend (BFF).
2.  **Backend (Deno)**: Orchestration, API Proxying, and Request Validation. No heavy simulation computation.
3.  **API Server (Kotlin)**: Core Business Logic, Simulation Engines (Interactive & Monte Carlo), and Database Access.
4.  **Data (SQLite)**: Accessed ONLY by the Kotlin API Server.

### III. Explicit Contracts & Type Safety
Data exchange between tiers (TypeScript Frontend/BFF <-> Kotlin API) must be defined by strict contracts. Changes to API response structures require simultaneous updates to:
1.  Kotlin Data Classes (`api-server`)
2.  TypeScript Interfaces (`backend` and `frontend`)

### IV. Deterministic Simulation
Simulation logic must be implemented as pure functions where possible. Given the same `SimulationConfig` inputs (and random seed for Monte Carlo), the system MUST produce the exact same `SimulationResult` every time.

## Development Standards

### Technology Specifics
-   **Kotlin**: Follow standard Kotlin conventions. Use `data classes` for immutable models. Use Ktor for routing.
-   **TypeScript**: Strict mode enabled. Avoid `any`. Use interfaces for data models.
-   **Deno**: Use standard library and Oak framework.

### Testing Strategy
-   **Domain Logic**: JUnit 5 through `kotlin-test-junit5` (Kotlin). Every branch and outcome of the spending
    strategy, the spending policies and the simulation engine logic MUST have a unit test that would fail
    without it. Reviewers check this rule against each change. No coverage-percentage tool is required.
-   **Integration**: `deno test` verifies BFF routing and error handling.
-   **UI**: Vitest verifies Component rendering and State management.

### Identifiers
Every identifier in a spec, plan, task list, checklist or analysis carries its spec's 3-digit number, so it is unique across all specs: `<KIND><spec>-<number>`.

| Kind | Example | Used for |
|------|---------|----------|
| `FR` | `FR004-050` | Functional requirement |
| `SC` | `SC004-001` | Success criterion |
| `US` | `US004-1` | User story (task labels: `[US004-1]`) |
| `T` | `T004-001` | Task |
| `R` | `R004-1` | Research decision |
| `CHK` | `CHK004-001` | Checklist item |
| `UI`, `TC`, … | `UI002-001` | Other requirement groups |
| Analysis findings | `C004-1`, `I004-2` | Category initial + spec number |

- Numbers are sequential within a spec and never reused. Suffixes stay as they are (`FR002-001a`, `T003-082b`, `FR003-033-v2`, `T002-041.4`).
- A reference to another spec uses that spec's identifier as is (`FR003-033-v2`), with no extra qualifier.
- Code comments, test names, docs and release notes cite the full identifier.
- Identifiers from before this rule (2026-10-16) were renumbered by prefixing the spec number; git history keeps the old form.

## Governance
This constitution guides all `speckit` workflows. Conflicts between this document and specific plans must be resolved in favor of this Constitution.

**Version**: 1.2 | **Ratified**: 2026-01-07 | **Last Amended**: 2026-10-19 (Testing Strategy: per-branch rule)