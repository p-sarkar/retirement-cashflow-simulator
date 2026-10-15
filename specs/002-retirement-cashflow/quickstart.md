# Quickstart

## Prerequisites
- Node.js 18+ & npm
- Deno 2.x
- JDK 25 (Gradle is provided by `./gradlew`)
- SQLite3 (optional CLI tool for debugging)

## Setup

1. **Install Frontend Dependencies**:
   ```bash
   cd frontend
   npm install
   ```

2. **Setup Deno Backend**:
   ```bash
   cd backend
   # No install needed for Deno usually, but check for lockfile
   deno cache src/main.ts
   ```

3. **Setup Kotlin API Server**:
   ```bash
   cd api-server
   ./gradlew build
   ```

## Running the Application

You need to run all three services.

**1. Kotlin API Server (Port 8090)**
```bash
cd api-server
./gradlew run
```

**2. Deno Backend (Port 8000)**
```bash
cd backend
deno task start
```

**3. Frontend (Port 5173)**
```bash
cd frontend
npm run dev
```

Open [http://localhost:5173](http://localhost:5173) to view the application.

## Try It

1. **Interactive simulation**: adjust the form (or pick **Market Conditions**, e.g. *2008 Crash*) and click **Run Simulation**. Toggle Yearly/Quarterly; **Export CSV** downloads the current view.
2. **Save and load**: click **Save** on the results and name the simulation. The **Saved Simulations** tab lists saved runs; **Load** puts the inputs back in the form and shows the saved results.
3. **Monte Carlo**: click **Run Monte Carlo (1,000 runs)**. Hover the chart to highlight a run, click it for its summary, then **Load Details** for its market variables and full cash flow table.