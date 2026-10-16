---
description: Generate an actionable, dependency-ordered tasks.md for the feature based on available design artifacts.
handoffs:
  - label: Analyze For Consistency
    agent: speckit.analyze
    prompt: Run a project analysis for consistency
    send: true
  - label: Implement Project
    agent: speckit.implement
    prompt: Start the implementation in phases
    send: true
strategy: wrap
---

{CORE_TEMPLATE}

## Identifiers (project preset `spec-numbered-ids`)

> **This section supersedes any identifier format shown above.** The examples above use spec kit's default
> identifiers (`T001`, `FR-001`, `US1`, `CHK001`). This project makes every identifier unique across specs by
> adding the spec number (constitution, "Identifiers"). Use the format below.

Task IDs and story labels carry this feature's 3-digit spec number (the `NNN` in `specs/NNN-short-name`):

- **Task ID**: `T`, the spec number, a hyphen and a sequential number: `T004-001`, `T004-002`, … in execution order
- **Story label**: `[US004-1]`, `[US004-2]`, …
- Requirements and criteria cited in task descriptions use their full identifiers (`FR004-050`, `SC004-001`)

Correct format, for spec 004:

- ✅ `- [ ] T004-001 Create project structure per implementation plan`
- ✅ `- [ ] T004-012 [P] [US004-1] Create User model in src/models/user.py`
- ❌ `- [ ] T001 [US1] Create User model in src/models/user.py` (missing spec number)
