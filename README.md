# Retirement Cash Flow Simulator

Web application for retirement planning that simulates 35-year cash flow projections (2025-2059) using market conditions, with deterministic and Monte Carlo analysis capabilities.

## Architecture

- **Frontend**: React + Vite + TypeScript + Material UI (port 5173)
- **Backend**: Deno + Oak gateway (port 8000)
- **API Server**: Kotlin + Ktor + Gradle (port 8090)
- **Database**: SQLite (accessed via API Server)

## One-Time Expenses

Besides recurring expenses, a simulation can include one-time expenses, entered under **One-Time Expenses** in the form (spec: `specs/003-one-time-expenses/`).

| Type | What you enter | How it is simulated |
|------|----------------|---------------------|
| **Cash** | Name, amount, age or calendar year | Paid as a lump sum from the Spend Bucket in Q1 of that year |
| **Loan** | Name, principal, optional down payment, APR, term (years), start age or year | Down payment paid in Q1 of the start year, then a fixed quarterly payment every quarter of the term |

- **Amounts are in today's dollars.** Cash amounts and down payments are inflated to the year they are paid.
- **Loan payments are fixed nominal amounts.** The API server inflates the financed amount (principal minus down payment) to the start year, then amortizes it with quarterly compounding: `Q = P·r(1+r)^n / ((1+r)^n − 1)`, where `r = APR / 4` and `n = 4 × term`. A 0% APR loan uses `Q = P / n`. The payment stays the same every quarter of the term and is not adjusted for inflation. The form's **Est. Quarterly Payment** is the same formula in today's dollars, shown only as a guide.
- **Timing.** Expenses run after regular expenses each quarter, in the order you entered them. If the Spend Bucket can't cover an expense, the normal spending strategy refills it (CBB, then TBA/TDA). A loan that runs past the last simulated year (age 85) only shows payments up to that year.
- **Results.** The results table has a **One-Time Expenses** column. The 🔍 breakdown for a year lists each expense by name and type (Cash Expense or Loan Payment), with the down payment as its own row. Loans show their annual total.
- **Validation.** The form won't run while any expense is incomplete or invalid. The API server also validates expenses and returns `422` with per-field errors (APR ≥ 0, term ≥ 1 year, 0 ≤ down payment ≤ principal, timing no earlier than the current year and no more than 35 years ahead).
- **Logging.** To log each expense's resolved years and simulated loan payment, set `com.retirement.logic.SimulationEngine` to `DEBUG` in `api-server/src/main/resources/logback.xml`.

## Development Setup

### Prerequisites
- Node.js 18+
- Deno
- Java 25 (for Kotlin)
- Gradle

### Running Services

```bash
# Frontend
cd frontend && npm run dev

# Backend (Deno gateway)
cd backend && deno task dev

# API Server (Kotlin)
cd api-server && ./gradlew run
```

## 🚨 CRITICAL: API Server Restart Protocol

**FOR COPILOT AGENTS AND DEVELOPERS:**

### ⚠️ ALWAYS restart the API server after ANY code changes in `api-server/`

This includes changes to:
- Any `.kt` Kotlin source files
- `build.gradle.kts`
- Data models (`SimulationResult.kt`, `SimulationConfig.kt`, etc.)
- Business logic (`SimulationEngine.kt`, `SpendingStrategy.kt`, etc.)
- Controllers, routes, or application configuration
- Resources or configuration files

### Restart Commands (Execute in Order)

```bash
# 1. Stop the API server
lsof -ti:8090 | xargs kill -9 2>/dev/null

# 2. Start the API server
cd api-server && nohup ./gradlew run > apiserver.log 2>&1 &

# 3. Wait and verify it's running
sleep 10 && lsof -i:8090
```

### Why This Is Critical

The Kotlin API server does NOT support hot-reload. Changes to the code are **NOT reflected** until the server process is restarted. Failing to restart will result in:
- ❌ Old code still running
- ❌ API returning stale data structures
- ❌ TypeErrors in frontend when data models change
- ❌ Confusing debugging sessions

### When to Restart

✅ **ALWAYS** restart after:
- Modifying any Kotlin file
- Adding/removing fields from data classes
- Changing API endpoints
- Updating business logic
- Modifying dependencies in `build.gradle.kts`

❌ **NO restart needed** for:
- Frontend-only changes (React/TypeScript)
- Backend-only changes (Deno)
- Documentation updates

## Project Structure

```
retirement-cash-flow-simulator/
├── api-server/           # Kotlin + Ktor API (port 8090)
│   ├── src/main/kotlin/
│   │   └── com/retirement/
│   │       ├── Application.kt
│   │       ├── logic/
│   │       │   ├── SimulationEngine.kt
│   │       │   └── SpendingStrategy.kt
│   │       └── model/
│   │           ├── SimulationConfig.kt
│   │           └── SimulationResult.kt
│   ├── build.gradle.kts
│   └── retirement.db      # SQLite database
├── backend/              # Deno + Oak gateway (port 8000)
│   └── src/
├── frontend/             # React + Vite + MUI (port 5173)
│   └── src/
│       ├── components/
│       │   ├── SimulationForm.tsx
│       │   └── ResultsTable.tsx
│       ├── types/
│       │   └── simulation.ts
│       └── services/
│           └── api.ts
├── historical-data/      # Market data CSVs
├── specs/               # Feature specifications
└── docs/                # Documentation
```

## Building

```bash
# Build API Server
cd api-server && ./gradlew build

# Build Frontend
cd frontend && npm run build

# Build Backend
cd backend && deno task build
```

## Testing

```bash
# Test API Server
cd api-server && ./gradlew test

# Test Frontend
cd frontend && npm test

# Test Backend (BFF)
cd backend && deno task test

# Lint
cd frontend && npm run lint
```

## API Contract Types

The TypeScript API types in `frontend/src/types/contract.generated.ts` and `backend/src/contract.generated.ts`
are generated from the Kotlin `@Serializable` models. Never edit them by hand. After changing a Kotlin
model, regenerate them:

```bash
cd api-server && ./gradlew generateTsContract
```

`./gradlew test` fails (`ContractTypesTest`) if the generated files are out of date.

## Troubleshooting

### API Server Won't Start (Gradle Daemon Crashes)

If you see `SIGSEGV` errors or Gradle daemon crashes:

```bash
# Use --no-daemon flag
cd api-server && ./gradlew run --no-daemon
```

### Changes Not Reflected

**Did you restart the API server?** See the restart protocol above.

### Port Already in Use

```bash
# Kill process on specific port
lsof -ti:8090 | xargs kill -9  # API server
lsof -ti:8000 | xargs kill -9  # Backend
lsof -ti:5173 | xargs kill -9  # Frontend
```

## Contributing

1. Make your changes
2. **If you modified `api-server/`**: RESTART THE API SERVER (see above)
3. Test your changes
4. **Before committing**: Increment the build number and rebuild:
   ```bash
   cd api-server
   ./gradlew incrementBuildNumber
   ./gradlew build
   ```
   This increments the version in `version.properties` and generates updated version info.
5. Restart the app to verify the new build:
   ```bash
   # Stop all services
   lsof -ti:8090 | xargs kill -9 2>/dev/null
   lsof -ti:8000 | xargs kill -9 2>/dev/null
   lsof -ti:5173 | xargs kill -9 2>/dev/null
   
   # Start all services
   cd api-server && nohup ./gradlew run > apiserver.log 2>&1 &
   cd backend && nohup deno task dev > backend.log 2>&1 &
   cd frontend && nohup npm run dev > frontend.log 2>&1 &
   
   # Verify
   sleep 12 && lsof -i:8090 && lsof -i:8000 && lsof -i:5173
   ```
6. **Commit with clear messages** (HUMAN DECISION ONLY)

### ⚠️ CRITICAL: Never Auto-Commit

**Commits must ALWAYS be a human decision. NEVER automate commits.**

- ❌ DO NOT create scripts that auto-commit
- ❌ DO NOT use git hooks that auto-commit
- ❌ DO NOT configure AI agents to auto-commit
- ✅ DO manually review all changes with `git diff`
- ✅ DO write meaningful commit messages
- ✅ DO verify changes are correct before committing

**Why?** Commits are permanent records in version control. They require human judgment to:
- Verify correctness of changes
- Write meaningful commit messages
- Ensure no sensitive data is included
- Group related changes appropriately
- Maintain clean git history

## License

MIT License

