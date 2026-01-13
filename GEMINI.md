# retirement-cash-flow-simulator Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-01-06

## Active Technologies
- SQLite (accessed via Kotlin API Server) (002-retirement-cashflow)

- TypeScript 5.x, Node.js 18+ + Next.js 14 (App Router), React 18, Material UI (MUI), Recharts (002-retirement-cashflow)

## Project Structure

```text
src/
tests/
```

## Commands

npm test && npm run lint

## Code Style

TypeScript 5.x, Node.js 18+: Follow standard conventions

## Recent Changes
- 002-retirement-cashflow: Added SQLite (accessed via Kotlin API Server)

- 002-retirement-cashflow: Added TypeScript 5.x, Node.js 18+ + Next.js 14 (App Router), React 18, Material UI (MUI), Recharts

<!-- MANUAL ADDITIONS START -->
* If any command fails with a Segmentation Fault, retry the command up to 3 times
* **🚨 CRITICAL: ALWAYS restart the api-server after ANY code changes in api-server/**
  - This is MANDATORY - changes to Kotlin code, data models, business logic, build files, or resources require a server restart
  - The API server does NOT support hot-reload - old code continues running until restart
  - Commands to restart (execute in order):
    1. Stop: `lsof -ti:8090 | xargs kill -9 2>/dev/null`
    2. Start: `cd api-server && nohup ./gradlew run > apiserver.log 2>&1 &`
    3. Verify: `sleep 10 && lsof -i:8090`
  - Do NOT skip this step or changes will not take effect
* Ensure code is formatted according to the project's linting rules before committing
<!-- MANUAL ADDITIONS END -->
