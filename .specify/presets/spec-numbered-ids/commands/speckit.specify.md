---
description: Create or update the feature specification from a natural language feature description.
handoffs:
  - label: Build Technical Plan
    agent: speckit.plan
    prompt: Create a plan for the spec. I am building with...
  - label: Clarify Spec Requirements
    agent: speckit.clarify
    prompt: Clarify specification requirements
    send: true
strategy: wrap
---

{CORE_TEMPLATE}

## Identifiers (project preset `spec-numbered-ids`)

> **This section supersedes any identifier format shown above.** The examples above use spec kit's default
> identifiers (`T001`, `FR-001`, `US1`, `CHK001`). This project makes every identifier unique across specs by
> adding the spec number (constitution, "Identifiers"). Use the format below.

Every identifier in `spec.md` carries this feature's 3-digit spec number: the `NNN` in the feature
directory name (`specs/NNN-short-name`). For spec 004:

- Functional requirements: `FR004-001`, `FR004-002`, …
- Success criteria: `SC004-001`, …
- User stories: `US004-1`, `US004-2`, … (headings stay "User Story 1 - …"; cite them as `US004-1`)
- Other requirement groups follow the same shape (`UI004-001`)

Replace `###` in the spec template's examples with the spec number. Cite another spec's identifiers as they are
(`FR003-033-v2`), without "spec 003".
