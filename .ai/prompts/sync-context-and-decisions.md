# Executable Protocol: Context, ADR & Machine State Drift Synchronization

This document defines the mandatory, deterministic execution protocol for auditing repository state, detecting documentation drift, and synchronizing `.ai/context/`, `.ai/decisions/`, `.ai/project-state.yaml`, and `CHANGELOG.md` with the latest code changes.

---

## 1. Preconditions

Before running synchronization, the AI agent **MUST** verify:
1. `git status` is clean on `master` (no unstaged user edits).
2. The IntelliJ project is open and the `idea` MCP server is responsive.
3. Gradle wrapper (`.\gradlew.bat` / `./gradlew`) is available.

---

## 2. Sequential Steps

The agent must execute these 8 steps in exact chronological order:

```text
[Step 1: Workspace Isolation] ── Create branch ai/sync-context
        │
        ▼
[Step 2: Git History Audit] ─── Inspect git log -n 15 and code diffs
        │
        ▼
[Step 3: Test Verification] ─── Run scripts/verify-patch.ps1 -Quick
        │
        ▼
[Step 4: Machine State Sync] ── Update .ai/project-state.yaml metrics & ADRs
        │
        ▼
[Step 5: Narrative State Sync] ─ Update .ai/context/current-state.md
        │
        ▼
[Step 6: Architecture & ADRs] ── Update architecture.md & decisions/README.md
        │
        ▼
[Step 7: Changelog Audit] ───── Verify ## [Unreleased] in CHANGELOG.md
        │
        ▼
[Step 8: Commit, Merge & Clean] Stage, commit, merge to master, delete branch
```

### Step 1: Workspace Isolation
- Create and switch to a dedicated task branch:
  ```bash
  git checkout -b ai/sync-context
  ```

### Step 2: Git History & Drift Inspection
- Run `git log -n 15 --oneline` to inspect recent commits.
- Identify newly added features, bug fixes, refactorings, or extension points that lack documentation in `.ai/`.

### Step 3: Verification & Test Extraction
- Run `powershell -ExecutionPolicy Bypass -File .\scripts\verify-patch.ps1 -Quick`.
- Verify compilation and plugin structure pass cleanly.

### Step 4: Synchronize Machine State (`.ai/project-state.yaml`)
- Update `metrics.passing_tests`, `metrics.test_suites`.
- Update `phases` status for any phase whose features were implemented or completed.
- Synchronize BOTH `architecture.adrs` and `architecture.decisions` dictionaries with all ADRs in `.ai/decisions/`.

### Step 5: Synchronize Narrative Current State (`.ai/context/current-state.md`)
- Update the Test Metrics summary and the Test Suite Breakdown table.
- Update the Feature Implementation Status section with recently delivered capabilities.
- Update Known Limitations (removing resolved issues).
- Anti-Truncation Guard: Verify the entire document remains complete from top to bottom.

### Step 6: Synchronize Architectural Design (`.ai/context/architecture.md`)
- If new subsystems, services, tool windows, run producers, or AST wrappers were added, update the architectural pipeline diagram and subsystem descriptions.
- Update Section 3 Extension Points catalog to match `plugin.xml`.
- Update Section 4 Test Structure table with current metrics.

### Step 7: Audit Public User Changelog (`CHANGELOG.md`)
- Verify all recent user-visible changes are recorded under `## [Unreleased]`.
- Enforce the user-facing standard: answers *"What changed for me?"* with zero class names, test counts, or ADR numbers.

### Step 8: Atomic Commits, Merge to Master, & Cleanup
- Stage and commit documentation drift fixes:
  ```bash
  git add .ai/project-state.yaml .ai/context/current-state.md .ai/context/architecture.md CHANGELOG.md
  git commit -m "docs(context): synchronize project state, architecture guide, and current state"
  ```
- Switch to master, merge, and clean up:
  ```bash
  git checkout master
  git merge --ff-only ai/sync-context
  git branch -d ai/sync-context
  ```

---

## 3. Mandatory Gates & Evidence Requirements

| Gate | Requirement | Mandatory Evidence in Agent Output |
|:---|:---|:---|
| **Gate 0: Isolation** | Branch created | Branch name `ai/sync-context` |
| **Gate 1: Verification** | Quick check passes | `build/verification-report.json` with status `"PASSED"` |
| **Gate 2: State Sync** | YAML metrics updated | Exact values for `passing_tests` and `test_suites` |
| **Gate 3: Current State** | Markdown updated | Updated sections in `current-state.md` |
| **Gate 4: Re-Read Verification** | Anti-assumption check | Explicit confirmation that all updated files were re-read and validated |
| **Gate 5: Clean Merge** | Master updated | `git status` clean on master |

---

## 4. Failure Conditions

Execution **MUST HALT** if:
1. `build/verification-report.json` indicates compilation or plugin structure failure.
2. Unreconciled discrepancies exist between `plugin.xml` extension points and code.
3. Merge conflicts occur when merging to `master`.

---

## 5. Recovery Rules

- **If Compilation Fails**: Identify the broken commit from `git log`, fix the syntax error, and re-run `verify-patch.ps1`.
- **If Extension Point is Missing in `plugin.xml`**: Add the missing `<extensionPoint>` or extension declaration before proceeding.

---

## 6. Completion Conditions

The task is complete **ONLY** when:
1. `project-state.yaml`, `current-state.md`, and `architecture.md` are aligned with actual repository state.
2. `CHANGELOG.md` under `## [Unreleased]` accurately reflects recent user changes.
3. Changes are committed, merged to `master`, and task branch deleted.
