# Research: Retirement Cash Flow Simulator

**Feature**: `002-retirement-cashflow`
**Date**: 2026-01-07

## 1. Architecture: N-Tier with BFF

**Decision**: Adopt a "Backend for Frontend" (BFF) pattern.
- **Frontend (React)** talks *only* to **Backend (Deno)**.
- **Backend (Deno)** validates requests, handles simple state if necessary, and proxies complex simulation requests to **API Server (Kotlin)**.
- **API Server (Kotlin)** executes the business logic and manages the **SQLite** database.

**Rationale**:
- Satisfies the user's explicit request for separate Deno and Kotlin layers.
- Decouples the UI from the heavy computation engine.
- Allows Deno to handle lightweight tasks (like serving the app or simple aggregation) while Kotlin focuses on raw performance for Monte Carlo simulations.

## 2. Kotlin Framework: Ktor

**Decision**: Use **Ktor** with Gradle.

**Rationale**:
- **Lightweight & Async**: Ktor is built on Kotlin Coroutines, making it highly efficient for I/O and non-blocking operations, matching the modern, async nature of the Deno/React stack.
- **Idiomatic**: It allows for a cleaner, DSL-based configuration compared to Spring Boot's annotation-heavy approach.
- **Performance**: Lower overhead than Spring Boot, which is beneficial for a focused "API Server".

**Alternatives Considered**:
- **Spring Boot**: Rejected due to higher weight and complexity ("magic") which might be overkill for this specific computation engine role.

## 3. Database Access: Exposed

**Decision**: Use **Exposed** (JetBrains) as the ORM/SQL framework.

**Rationale**:
- **Type Safety**: Provides a strongly-typed DSL for SQL generation.
- **Kotlin-First**: Designed specifically for Kotlin, unlike Java-based ORMs like Hibernate.
- **SQLite Support**: Good support for SQLite dialects.

## 4. Inter-Service Communication

**Decision**: REST (JSON over HTTP).

**Rationale**:
- **Simplicity**: Easy to debug and implement in both Deno (Oak) and Kotlin (Ktor).
- **Compatibility**: Standard for web architectures.

## 5. Testing Strategy

**Decision**:
- **Frontend**: Vitest (Unit/Component), Playwright (E2E if needed later).
- **Backend (Deno)**: Native `deno test`.
- **API Server (Kotlin)**: JUnit 5 with Mockk (for mocking) and Ktor Client (for integration tests).

## 6. Monte Carlo Performance

**Findings**:
- Running 1000 simulations for 35 years involves ~35,000 yearly calculations.
- Kotlin (JVM) is well-suited for this.
- **Optimization**: Use primitive arrays or specialized structures in Kotlin if object overhead becomes too high, but standard objects should suffice for < 10s goal.
- **Concurrency**: Use Kotlin Coroutines (`Dispatchers.Default`) to parallelize the 1000 runs.