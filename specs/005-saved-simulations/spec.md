# Feature Specification: Saved Simulations Improvements

**Feature Branch**: `feat/005-saved-simulations`
**Created**: 2026-10-18
**Status**: Implemented (2026-10-19)
**Input**: User description: "Saved simulations improvements. (1) Stale results: a saved simulation's results were computed by the API version recorded in apiMetadata.version; when a simulation saved by an older version is loaded, flag that its results came from an older version and let the user re-run it with the current version, keeping the saved results available for comparison until the user saves again. (2) Manage saved simulations: update (overwrite) a saved simulation with the current inputs and results, rename it, and delete it, with confirmation for delete. (3) Export and import inputs: download a saved simulation's or the current form's inputs as a JSON file, and import such a file to fill the form, validated by the API like any other config (lower priority). Out of scope: keeping an unsaved form draft across page reloads; saving Monte Carlo analyses (reproducible from inputs and seed)."

Builds on saved simulations in spec 002 (FR002-011 to FR002-013): a simulation is saved with a name, its inputs and its results; the list shows each one's name, date run, outcome, ending balance and End Age; Load fills the form with the inputs and shows the results exactly as saved (SC002-004).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Know when saved results are out of date, and refresh them (Priority: P1)

The simulator's rules change between versions (v1.1.29 changed taxes and the spending strategy). A simulation saved
before such a change still shows the results the old version computed, with nothing to say so. The user loads a
saved plan, sees that its results came from an older version, re-runs it with the current version in one step, and
compares the new results with the saved ones before deciding whether to keep them.

**Why this priority**: Without it, a user can make decisions on results the simulator no longer stands behind. Every
simulation saved before v1.1.29 is affected today.

**Independent Test**: Load a simulation saved by an older version: it is flagged, with both version numbers. Re-run
it: the new results match a fresh run of the same inputs, and the saved results can still be viewed until the user
saves.

**Acceptance Scenarios**:

1. **Given** a simulation whose results were computed by v1.1.28, **When** the user opens the saved simulations list
   on v1.1.29, **Then** that entry is marked as computed by an older version.
2. **Given** that simulation, **When** the user loads it, **Then** the saved results are shown, labelled with the
   version that computed them, with a notice that the current version may give different results and an action to
   re-run.
3. **Given** the notice, **When** the user re-runs, **Then** the results of the current version are shown, along with
   how the outcome, failure year and ending balance differ from the saved results.
4. **Given** re-run results, **When** the user switches to the saved results and back, **Then** each view shows its own
   results unchanged, and the stored simulation is unchanged.
5. **Given** a simulation computed by the current version, **When** it is listed or loaded, **Then** it carries no flag
   and no re-run notice.

---

### User Story 2 - Keep the saved list current: update, rename, delete (Priority: P2)

The user adjusts a saved plan and wants to keep the improved version under the same name rather than add another
entry, gives a plan a clearer name, or removes plans they no longer need, without being able to delete one by
accident.

**Why this priority**: Today saved simulations can only be added, so the list grows with outdated copies and
re-running an old plan (US005-1) can't be kept without creating a duplicate.

**Independent Test**: Load a saved simulation, change an input, run, and update it: the list still has one entry with
that name, now with the new inputs, results and update date. Rename and delete an entry; delete asks for
confirmation first.

**Acceptance Scenarios**:

1. **Given** results shown for a loaded saved simulation (re-run or after edited inputs), **When** the user saves,
   **Then** they can either update that simulation or save a new one; updating replaces its inputs and results,
   keeps its name and creation date, and records when it was updated.
2. **Given** results from inputs that weren't loaded from a saved simulation, **When** the user saves, **Then** a new
   simulation is saved, as today.
3. **Given** a saved simulation, **When** the user renames it to a non-blank name, **Then** the list shows the new name
   and nothing else about it changes.
4. **Given** a saved simulation, **When** the user chooses delete, **Then** they are asked to confirm, naming the
   simulation; only on confirming is it removed, permanently.
5. **Given** a saved simulation that is currently loaded, **When** the user deletes it, **Then** its inputs and results
   stay on screen as unsaved, and saving can only create a new simulation.

---

### User Story 3 - Export and import inputs as a file (Priority: P3)

The user downloads a plan's inputs to a file, as a backup independent of the app's local storage, to move it to
another machine or to share it, and later imports the file to fill the form.

**Why this priority**: Valuable for backup and sharing, but the app works without it; P1 and P2 cover day-to-day use.

**Independent Test**: Export the inputs of a saved simulation, import the file into an empty form, and run: the
results are identical to the saved simulation's when re-run. Import a file with an invalid value: the form is
unchanged and every problem is listed.

**Acceptance Scenarios**:

1. **Given** the form's inputs or a saved simulation, **When** the user exports, **Then** a file named after the
   simulation is downloaded, holding its inputs only (no results).
2. **Given** an exported file, **When** the user imports it, **Then** the form is filled with its inputs, checked by the
   same rules as running a simulation, and nothing is run or saved until the user chooses to.
3. **Given** a file with invalid inputs (e.g. Retirement Age outside 18–110), **When** the user imports it, **Then** the
   form is unchanged and each invalid field is listed with its problem.
4. **Given** a file that isn't an exported inputs file (wrong content, unreadable, or too large), **When** the user
   imports it, **Then** the form is unchanged and the user is told the file can't be used.
5. **Given** a file exported by an older version that lacks a newer input (e.g. End Age), **When** imported, **Then**
   the missing input takes its default, as for older saved simulations.

### Edge Cases

- **No version recorded**: a saved simulation without a recorded version is treated as computed by an older version
  ("unknown version").
- **Newer version recorded** (the app was downgraded): flagged as computed by a different version, the same way.
- **Re-run fails validation** (inputs valid when saved but rejected by the current rules): the saved results stay
  shown, and the field errors are shown as for any run.
- **Simulation removed elsewhere** (deleted in another browser tab before an update, rename, delete or load): the user
  is told it no longer exists and the list is refreshed; an update can still be saved as new.
- **Edited inputs not yet run**: saving always saves the inputs of the results shown, never unrun form edits, as today.
- **Monte Carlo after a re-run**: a Monte Carlo analysis shown for the saved inputs stays valid (same inputs); one run
  for edited inputs belongs to those inputs, as today.
- **Duplicate names**: allowed, as today; entries are told apart by their dates.
- **Blank or very long rename**: a blank name is rejected; names are trimmed and limited to 255 characters, the same rule as saving.

## Requirements *(mandatory)*

### Functional Requirements

#### Stale results (US005-1)

- **FR005-001**: The saved simulations list MUST mark each simulation whose results were computed by a version other
  than the running one, or by an unknown version.
- **FR005-002**: Loading such a simulation MUST show its saved results labelled with the version that computed them,
  with a notice naming both versions and stating that the current version may give different results.
- **FR005-003**: The notice MUST offer a re-run that runs the saved inputs with the current version and shows its
  results, as a fresh run of the same inputs would.
- **FR005-004**: After a re-run, the user MUST be able to switch between the re-run results and the saved results,
  and MUST see the difference in outcome (success or failure), failure year and ending total balance.
- **FR005-005**: A re-run MUST NOT change the stored simulation; the saved results remain available for comparison
  until the user updates the simulation (FR005-007), loads another simulation or runs other inputs.
- **FR005-006**: Simulations computed by the running version MUST NOT be flagged and MUST NOT show the notice.

#### Manage saved simulations (US005-2)

- **FR005-007**: When the results shown come from a loaded saved simulation (re-run or after edited inputs), saving
  MUST offer to update that simulation or save a new one. Updating MUST replace its inputs and results with those of
  the results shown, keep its name and creation date, and record the update date and the version that computed the
  new results.
- **FR005-008**: Users MUST be able to rename a saved simulation. Names MUST be trimmed, non-blank and at most 255
  characters (the same rule as saving, FR002-011); duplicates are allowed.
- **FR005-009**: Users MUST be able to delete a saved simulation after confirming in a prompt that names it. Deletion
  is permanent.
- **FR005-010**: Deleting the loaded simulation MUST leave its inputs and results on screen as unsaved; saving them
  then creates a new simulation.
- **FR005-011**: The list MUST show each simulation's last update date (its creation date if never updated) and be
  ordered most recently changed first.
- **FR005-012**: An update, rename, delete or load of a simulation that no longer exists MUST tell the user so and
  refresh the list; for an update, the user MUST be able to save the results as a new simulation instead.

#### Export and import inputs (US005-3)

- **FR005-013**: Users MUST be able to export the inputs of the form or of a saved simulation to a file named after
  the simulation. The file MUST contain the inputs only, with the app version that wrote it and a file format version.
- **FR005-014**: Users MUST be able to import such a file. Its inputs MUST be checked by the same rules as running a
  simulation. Valid inputs fill the form without running or saving anything.
- **FR005-015**: An import with invalid inputs MUST leave the form unchanged and list every invalid field with its
  problem at once.
- **FR005-016**: A file that isn't an exported inputs file, can't be read, or is larger than 1 MB MUST be rejected
  with a message, leaving the form unchanged.
- **FR005-017**: Inputs missing from a file written by an older version MUST take their defaults (e.g. End Age 85), as
  for older saved simulations.
- **FR005-018**: Exporting inputs and importing the file MUST reproduce the same inputs, so running them gives the same
  results.

### Key Entities

- **Saved Simulation** (extends spec 002's SavedSimulation): name, creation date, last update date (new), inputs,
  results, and the version that computed the results (already recorded with the results).
- **Exported Inputs File** (new): a simulation's inputs, the app version that wrote it and a file format version; no
  results.
- **Results Comparison** (new, not stored): the saved and re-run results of one simulation, with their differences in
  outcome, failure year and ending total balance.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC005-001**: Every saved simulation whose results came from a different or unknown version is flagged, in the list
  and on load; none computed by the running version is.
- **SC005-002**: Refreshing a flagged simulation's results takes one action, and the results equal those of a fresh run
  of the same inputs.
- **SC005-003**: Updating, renaming or deleting a saved simulation takes at most two actions each, and no simulation is
  ever deleted without a confirmation.
- **SC005-004**: For every saved simulation and every market preset, exporting the inputs and importing the file gives
  identical inputs and, when run, identical results.
- **SC005-005**: An invalid import never changes the form, and lists all of its invalid fields at once.

## Assumptions

- **Single user, local**: as in spec 002, the app has one user and stores simulations locally; there are no
  permissions or sharing inside the app (sharing is by exported file).
- **Version**: "version" is the app version reported with every result (e.g. 1.1.29). Results depend only on the inputs
  and the version (constitution: deterministic simulation), so results from the running version never need a re-run.
- **No automatic migration**: stored results are never recomputed automatically; they change only when the user updates
  the simulation.
- **Existing simulations**: simulations saved before this feature have no update date; their creation date is used.
- **Delete is permanent**: there is no undo or trash; an exported file is the backup.
- **Out of scope**: keeping an unsaved form draft across page reloads; saving Monte Carlo analyses (reproducible from
  the inputs and seed); comparing two different saved simulations; importing results.
