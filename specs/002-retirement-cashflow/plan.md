# Implementation Plan: Retirement Cash Flow Simulator

**Branch**: `002-retirement-cashflow` | **Date**: 2026-01-07 | **Spec**: [specs/002-retirement-cashflow/spec.md](specs/002-retirement-cashflow/spec.md)
**Input**: Feature specification from `/specs/002-retirement-cashflow/spec.md`

## Summary

This feature implements a comprehensive retirement cash flow simulator with two main components: an interactive year-by-year simulation and a Monte Carlo analysis. The solution follows an N-tier architecture:
- **Frontend**: React (Vite, TypeScript, MUI) for visualization and user interaction.
- **Backend (BFF)**: Deno (TypeScript) with Oak, acting as the primary entry point for the frontend, handling orchestration.
- **API Server**: Kotlin with Gradle (Ktor) for high-performance simulation logic (especially Monte Carlo) and domain rules.
- **Data Store**: SQLite for persisting simulations and user configurations.

## Technical Context

**Language/Version**: 
- Frontend: TypeScript 5.x
- Backend: TypeScript (Deno)
- API Server: Kotlin (JVM)
**Primary Dependencies**: 
- Frontend: React 18, Vite, MUI, Recharts
- Backend: Oak (Deno framework)
- API Server: Ktor (Kotlin framework), Exposed (ORM)
**Storage**: SQLite (accessed via Kotlin API Server)
**Testing**: 
- Frontend: Vitest
- Backend: `deno test`
- API Server: JUnit 5
**Target Platform**: Web (Local execution/Server)
**Project Type**: N-tier Web Application
**Performance Goals**: 
- Interactive Sim: < 1s
- Monte Carlo (1000 runs): < 10s
**Scale/Scope**: Single user (initially), local persistence.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Standard Conventions**: Adhered to.
- **New Technologies**: Deno and Kotlin introduced as per user request. This adds complexity but aligns with "Performance Goals" (Kotlin for Monte Carlo) and "Modern Stack" (Deno).

**Result**: PASSED

## Project Structure

### Documentation (this feature)

```text
specs/002-retirement-cashflow/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
└── tasks.md             # Phase 2 output
```

### Source Code (repository root)

```text
frontend/               # React Application
├── src/
│   ├── components/     # UI Components (Charts, Forms)
│   ├── pages/          # Application Pages
│   └── services/       # API Clients (talking to Deno)
└── tests/

backend/                # Deno BFF
├── src/
│   ├── routes/
│   └── controllers/
└── tests/

api-server/             # Kotlin Simulation Engine
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   ├── model/  # Domain Logic & Entities
│   │   │   ├── api/    # Ktor Routes
│   │   │   └── data/   # Exposed/SQLite
│   │   └── resources/
└── build.gradle.kts
```

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| 3-Tier (Deno + Kotlin) | Explicit User Request for N-tier | User mandated "Backend: Deno... API Server: Kotlin" |
| Kotlin for Logic | Performance for Monte Carlo | Node.js might be slower for heavy computation loops (1000 runs * 35 years) |