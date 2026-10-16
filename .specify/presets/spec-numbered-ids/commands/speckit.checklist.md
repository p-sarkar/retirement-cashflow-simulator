---
description: Generate a custom checklist for the current feature based on user requirements.
strategy: wrap
---

{CORE_TEMPLATE}

## Identifiers (project preset `spec-numbered-ids`)

> **This section supersedes any identifier format shown above.** The examples above use spec kit's default
> identifiers (`T001`, `FR-001`, `US1`, `CHK001`). This project makes every identifier unique across specs by
> adding the spec number (constitution, "Identifiers"). Use the format below.

Checklist item IDs carry this feature's 3-digit spec number (the `NNN` in `specs/NNN-short-name`):
`CHK004-001`, `CHK004-002`, … When appending to an existing checklist, continue from its last number
(`CHK004-015` → `CHK004-016`). Spec references use full identifiers: `[Spec §FR004-001]`.
