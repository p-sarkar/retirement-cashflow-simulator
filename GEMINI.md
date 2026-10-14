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

## ⚠️ CRITICAL: NEVER AUTO-COMMIT

**Commits must ALWAYS be a human decision. AI agents must NEVER commit code automatically.**

### When User Asks You To:
- "Make a change" → Make the change, test it, **STOP** (do NOT commit)
- "Add a feature" → Add the feature, test it, **STOP** (do NOT commit)
- "Fix a bug" → Fix the bug, test it, **STOP** (do NOT commit)
- "Update the code" → Update the code, test it, **STOP** (do NOT commit)

### Only Commit When User EXPLICITLY Says:
- "commit" or "commit this" or "commit the changes"
- "commit and push"
- "save these changes"
- "git commit"

### Workflow:
1. **Make changes** as requested
2. **Test changes** (build, restart services if needed)
3. **Report completion** to user
4. **WAIT for user to explicitly request commit**
5. Only then: increment version, build, commit, push

### Example Good Behavior:
```
User: "Add a new field to the form"
Agent: [Makes change, tests]
Agent: "✅ Added new field. Tested successfully. Ready to commit when you're ready."
User: "commit"
Agent: [increments version, builds, commits, pushes]
```

### Example BAD Behavior (DO NOT DO THIS):
```
User: "Add a new field to the form"
Agent: [Makes change, tests, AND COMMITS WITHOUT ASKING]  ❌ WRONG!
```

## Why This Matters:
- Commits are permanent records
- User needs to review changes first
- Commit messages should be written by humans
- Version increments should be intentional
- Git history should be clean and meaningful

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
