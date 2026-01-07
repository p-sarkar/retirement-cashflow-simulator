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
-   **Domain Logic**: JUnit 5 (Kotlin) covers 100% of Spending Strategy and Calculation Engine logic.
-   **Integration**: `deno test` verifies BFF routing and error handling.
-   **UI**: Vitest verifies Component rendering and State management.

## Governance
This constitution guides all `speckit` workflows. Conflicts between this document and specific plans must be resolved in favor of this Constitution.

**Version**: 1.0 | **Ratified**: 2026-01-07