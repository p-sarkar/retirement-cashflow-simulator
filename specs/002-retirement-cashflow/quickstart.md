# Quickstart

## Prerequisites
- Node.js 18+ & npm
- Deno 1.x
- JDK 17+ & Gradle (or use `./gradlew`)
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

**1. Kotlin API Server (Port 8081)**
```bash
cd api-server
./gradlew run
```

**2. Deno Backend (Port 8000)**
```bash
cd backend
deno run --allow-net --allow-read src/main.ts
```

**3. Frontend (Port 5173)**
```bash
cd frontend
npm run dev
```

Open [http://localhost:5173](http://localhost:5173) to view the application.