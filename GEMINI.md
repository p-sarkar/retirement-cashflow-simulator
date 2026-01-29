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

## ⚠️ CRITICAL: Git Commit and Push Protocol

**READ THE FULL PROTOCOL**: See `CONSTITUTION.md` Section 2 for complete details.

### Core Rule

**Code must NEVER be committed or pushed without EXPLICIT user permission.**

### Quick Reference

**AI Can Do Freely**:
- Make code changes
- Run tests and builds
- Restart services
- Report completion

**AI Must Ask Permission**:
- ⛔ Committing code
- ⛔ Pushing to repository
- ⛔ Creating/merging branches

### Standard Workflow

1. User requests changes → Make changes, test, **STOP**
2. Report: "✅ Changes complete. Ready to commit when you say so."
3. Wait for user to explicitly say "commit" or "commit and push"
4. Only then: increment version, build, commit, push

### Only Commit When User Says

- "commit" / "commit and push"
- "save these changes"
- "increment version, commit and push"

**NEVER** commit automatically after making changes, even if tests pass.

**See `CONSTITUTION.md` for**:
- Detailed workflow examples
- Why this matters
- Complete pre-commit checklist
- Correct vs incorrect behavior patterns

## Other Instructions:

### Always Restart Services After Code Changes:
- API Server changes → Restart API server
- Frontend changes → Frontend auto-reloads (Vite HMR)
- Backend changes → Backend auto-reloads (Deno watch)

### Before Any Commit (When Explicitly Requested):
1. Run `./gradlew incrementBuildNumber`
2. Run `./gradlew build`
3. Restart services to verify
4. Then commit with clear message
5. Push to remote

### Never Auto-Execute:
- ❌ git commit
- ❌ git push
- ❌ npm publish
- ❌ Production deployments
- ❌ Database migrations in production
- ❌ Destructive operations without confirmation

<!-- MANUAL ADDITIONS END -->
