# Preset: spec-numbered-ids

Makes spec kit generate identifiers that carry the spec number (`FR004-050`, `T004-001`, `US004-1`,
`CHK004-001`), so they are unique across specs. The rule itself is in the constitution
(`.specify/memory/constitution.md`, "Identifiers"); this preset makes the templates and commands follow it.

This folder is the source. The installed copy is `.specify/presets/spec-numbered-ids/`; both are committed.

## What it provides

| Item | Strategy | Effect |
|------|----------|--------|
| `spec-template`, `tasks-template`, `checklist-template` | replace | Examples use `###` for the spec number (`FR###-001`, `T###-001`, `CHK###-001`) |
| `speckit.specify`, `.tasks`, `.checklist`, `.analyze`, `.converge` | wrap | The core command, followed by an "Identifiers" section that supersedes its example IDs |

`speckit.taskstoissues` is deprecated upstream and isn't wrapped.

## Requirements

Spec kit's template scripts need **PyYAML** once any preset is installed:

```bash
python3 -m pip install --user --break-system-packages pyyaml
```

Without it, `/speckit-specify`, `/speckit-plan` and `/speckit-tasks` fail with "PyYAML is required to resolve preset
template composition".

## Changing the preset

Edit the files here, then reinstall:

```bash
specify preset remove spec-numbered-ids
specify preset add --dev .specify/custom-presets/spec-numbered-ids
```

This regenerates the five wrapped skills in `.claude/skills/`. Commit the source, the installed copy and the
regenerated skills together.

## Upgrading spec kit

The wrapped skills differ from what spec kit installed, so `specify integration status` reports them as modified
and an upgrade needs `--force`. That is safe: after upgrading, spec kit re-applies installed presets.

```bash
specify integration upgrade claude --force
specify preset list                     # spec-numbered-ids is still installed
specify preset resolve spec-template    # top layer: spec-numbered-ids
```

Then check that `.claude/skills/speckit-tasks/SKILL.md` ends with the "Identifiers" section. If an upgrade changes
the command layout (commands ↔ skills), spec kit refuses while a preset is installed: remove it, upgrade, then add
it back (it prints these commands).

The upgrade also recreates `.specify/scripts/bash/`, which this project removed (it uses the Python scripts);
delete it again.
