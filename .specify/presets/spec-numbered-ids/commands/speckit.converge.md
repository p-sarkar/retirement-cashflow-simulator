---
description: Assess the current codebase against the feature's spec, plan, and tasks, then append any remaining unbuilt work as new tasks to tasks.md so implement can complete it.
strategy: wrap
---

{CORE_TEMPLATE}

## Identifiers (project preset `spec-numbered-ids`)

> **This section supersedes any identifier format shown above.** The examples above use spec kit's default
> identifiers (`T001`, `FR-001`, `US1`, `CHK001`). This project makes every identifier unique across specs by
> adding the spec number (constitution, "Identifiers"). Use the format below.

New task IDs continue the feature's existing ones, which carry its 3-digit spec number `S`: let `M` be
the highest `NNN` among the existing `T{S}-NNN` IDs, and number new tasks `T{S}-{M+1:03d}`, `T{S}-{M+2:03d}`, …
(for spec 004: `T004-045`, `T004-046`). Finding IDs are `F{S}-1`, `F{S}-2`, … and `<source-ref>` uses full
identifiers (`FR004-003`, `SC004-002`, `US004-1/AC2`).
