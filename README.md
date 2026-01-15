# Retirement Cash Flow Simulator

Web application for retirement planning that simulates 35-year cash flow projections (2025-2059) using market conditions, with deterministic and Monte Carlo analysis capabilities.

## Architecture

- **Frontend**: React + Vite + TypeScript + Material UI (port 5173)
- **Backend**: Deno + Oak gateway (port 8000)
- **API Server**: Kotlin + Ktor + Gradle (port 8090)
- **Database**: SQLite (accessed via API Server)

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

# Lint
cd frontend && npm run lint
```

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
4. **Before committing**: Run a full build to increment the build number:
   ```bash
   cd api-server && ./gradlew build
   ```
   This auto-increments the version in `version.properties` and generates updated version info.
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
6. Commit with clear messages

## License

MIT License

