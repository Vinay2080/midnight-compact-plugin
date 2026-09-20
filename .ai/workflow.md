# Task Lifecycle, Workflows, & Completion Gate

This document defines the strict, standard execution lifecycle for any software development, refactoring, bug-fixing, code review, release-time changelog cleanup, semantic tagging, and release task within `midnight-compact-plugin`.

---

## 1. Task Lifecycle Overview

Every development session modifying repository files must progress sequentially through this isolated lifecycle:

```text
SESSION START
  │
  ▼
[0. Workspace Isolation] ────────── Inspect git state, verify master, create dedicated branch ai/<slug>
  │
  ▼
[1. Context Loading & Pre-Debug] ── Load AGENTS.md + task rules; search .ai/bugs/ if fixing defects
  │
  ▼
[2. Inspect Implementation] ─────── Read code, tests, compiler refs, & reference plugins
  │
  ▼
[3. Plan & Align] ──────────────── Check invariants, consult ADRs, formulate minimal plan
  │
  ▼
[4. Implement] ─────────────────── Write modern Java 25 adhering to threading & PSI models
  │
  ▼
[5. Test & Validate] ────────────── Continuous inspection via MCP, run scripts/verify-patch.ps1
  │
  ▼
[6. Code Review Self-Check] ─────── Verify against Section 9 anti-bikeshedding & invariant bar
  │
  ▼
[7. Documentation Impact] ───────── Apply Intelligent Documentation Decision Rules (including .ai/bugs/)
  │
  ▼
[8. Update Documentation] ───────── Make targeted updates to applicable doc/state/bug files
  │
  ▼
[9. Verify Documentation] ───────── Re-read modified files (anti-assumption rule); confirm integrity
  │
  ▼
[10. Final Branch Verification] ─── Re-run verify-patch.ps1 (zero regressions, all gates pass)
  │
  ▼
[11. Commit Task Branch] ────────── Commit all verified changes to ai/<task-slug>
  │
  ▼
[12. Merge into Master & Verify] ── Switch to master, merge --ff-only, re-run verify-patch.ps1
  │
  ▼
[13. Cleanup & Final Gate] ──────── Delete temporary branch, emit Final Artifact Completion Table
  │
  ▼
SESSION COMPLETE
```

---

## 2. Workspace Isolation & Branching Standards

### 2.1 Core Principle: Non-Pollution of Master
`master` is the pristine, releasable baseline. Work is performed in isolation and merged back only when 100% complete, fully tested, documented, and verified.

> [!IMPORTANT]
> **Read-Only Exception**: Purely read-only tasks (answering questions, code navigation, architectural lookups that produce zero file changes) are exempt from creating a branch. Isolation is mandatory for any session that makes repository edits.

### 2.2 Branch Isolation Model in ACP / IDE Environments
- In IDE / ACP assistant environments, **in-workspace branch isolation** (`git checkout -b ai/<task-slug>`) is the standard model.
- Why in-workspace branching: IntelliJ MCP inspection tools (`get_file_problems`, `lint_files`) and client file tools operate strictly within the opened IDE project workspace boundary. External worktrees (`../midnight-plugin-wt-...`) reside outside that boundary and cause inspection tool lookup failures.
- If working purely from an external terminal or CLI without IDE MCP requirements, sibling worktrees may be used.

### 2.3 Safe Handling of Existing Changes
Before creating a branch or editing files:
1. Inspect `git status` on `master`.
2. **NEVER silently discard, reset, stash destructively, overwrite, or revert existing user modifications on `master`**.
3. Destructive commands (`git reset --hard`, `git clean -fd`) are strictly prohibited without explicit user instruction.

### 2.4 Branch Naming Standard
Every modifying session must establish a distinct identity:
- **Dedicated Branch**: `ai/<task-slug>` (e.g. `ai/fix-parser-recovery`, `ai/feat-export-ledger`).
- Prohibited generic names: `test`, `temp`, `branch1`, `work`, `patch`, `dev`.

---

## 3. Phase 1: Context Loading & Task Routing

Before proposing or editing anything, the agent **MUST** route the task using the 10-Class Matrix in [`.ai/prompts/README.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/README.md) and load the required context:

| Task Type | Mandatory Context Reading |
|:---|:---|
| **`BUG`** | [`.ai/prompts/bug-fix.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/bug-fix.md), [`.ai/bugs/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/bugs/), [`.ai/bugs/recurring-patterns.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/bugs/recurring-patterns.md), [`.agents/rules/architecture.rules.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.agents/rules/architecture.rules.md), relevant `src/test/` fixture, Compact compiler ground truth |
| **`FEATURE`** | [`.ai/prompts/feature-implementation.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/feature-implementation.md), [`.ai/project-state.yaml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/project-state.yaml), [`.ai/context/current-state.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/current-state.md), [`.agents/rules/architecture.rules.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.agents/rules/architecture.rules.md), [`.agents/rules/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.agents/rules/), reference plugins |
| **`ARCHITECTURE`** | [`.ai/prompts/architecture-decision.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/architecture-decision.md), [`.ai/decisions/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/), [`.ai/context/architecture.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/architecture.md), [`.agents/rules/architecture.rules.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.agents/rules/architecture.rules.md) |
| **`RELEASE`** | [`.ai/prompts/push-and-release.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/push-and-release.md), [`CHANGELOG.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/CHANGELOG.md), [`gradle.properties`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/gradle.properties) |
| **`MAINTENANCE`** | [`.ai/prompts/sync-context-and-decisions.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/sync-context-and-decisions.md), [`.ai/meta/drift-audit-ledger.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/meta/drift-audit-ledger.md) |

---

## 4. Phases 2–6: Execution & Verification

### Phase 2: Inspect Implementation & Ground Truth
- Never write code based on generic assumptions.
- Read targeted production classes and existing test fixtures before proposing edits.
- For bug fixes: **Search `.ai/bugs/` first**. Query by feature, class, or symptom.

### Phase 3: Plan & Architectural Alignment (Architectural Pre-Flight)
- **1. Semantic Root Layer**: Verify logic is placed in the correct layer (`type/` for types, `resolve/` for scopes, `completion/` for UI). Never fix a type issue in the completion layer.
- **2. The Generalization Rule**: Verify that the solution applies universally to **all** structs and types. Never special-case standard library names (`Either`, `Maybe`, `Vector`) in general compiler/completion logic.
- **3. Modularity & Size Threshold**: Verify the design keeps class files $\le$ 400 lines and methods $\le$ 40 lines. Split large contributors into dedicated `CompletionProvider` subclasses.
- **4. Invariant Verification**: Verify the plan obeys all Critical Invariants in `AGENTS.md` and [`.agents/rules/architecture.rules.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.agents/rules/architecture.rules.md).

### Phase 4: Implementation (Modern Java 25)
- Follow path-scoped rules in [`.agents/rules/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.agents/rules/):
  - Records (`record`) for immutable models, DTOs, and cache keys.
  - Sequenced Collections (`getFirst()`, `getLast()`, `reversed()`). Never use `get(0)`.
  - Pattern matching switch expressions with arrow syntax (`->`).
  - Unnamed variables `_` for unused parameters and exceptions.
  - Threading: `ReadAction` for PSI reads; EDT `WriteCommandAction` for PSI mutations; no blocking `waitFor()` on EDT.
  - Robustness: Guard against `null` and `PsiErrorElement`. Loops must advance lexer tokens.

### Phase 5: Test & Validate
- **Mandatory "User-Defined Mirror" Test**: If testing a stdlib construct (`Either`, `Maybe`), write an identical parallel test using a custom user-defined generic struct (`Result<TVal, TErr>`, `Pair<A, B>`).
- **Mandatory Post-Edit Code Inspection (Every Edit)**:
  - Run IntelliJ inspections via MCP immediately after EVERY edit:
    ```text
    execute_tool get_file_problems --filePath "<absolute-path>"
    execute_tool lint_files --files [\"<absolute-path>\"]
    ```
  - Zero-tolerance: 0 errors, 0 warnings, 0 weak warnings, 0 typos.
- **Deterministic Multi-Gate Verification**:
  - Execute the automated runner:
    ```powershell
    powershell -ExecutionPolicy Bypass -File .\scripts\verify-patch.ps1 -TestPattern "<target>"
    ```
  - Gate 0 (Git boundary), Gate 1 (Java 25 compile), Gate 2 (plugin structure), Gate 3 (tests).
  - Verify [`build/verification-report.json`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/build/verification-report.json) has status `"PASSED"`.

### Phase 6: Code Review Self-Check
- Evaluate changes against [`.agents/rules/architecture.rules.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.agents/rules/architecture.rules.md) and the anti-bikeshedding bar in `AGENTS.md`:
  - No cosmetic rewrites or subjective renames.
  - No unauthorized hardcoded type string checks.
  - Only introduce changes justified by correctness, threading safety, generalization, or bug fixes.

---

## 5. Phase 7: Intelligent Documentation Decision Rules

Evaluate each documentation artifact against these exact criteria:

| Artifact | When to Update | When NOT to Update |
|:---|:---|:---|
| **[`CHANGELOG.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/CHANGELOG.md)** | Add notable user-visible changes under `## [Unreleased]`, answering *"What changed for me?"* | Never record internal refactorings, test counts, class names, ADR numbers, or AI notes. |
| **Bug Record ([`.ai/bugs/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/bugs/))** | Concrete defect investigated, fixed, and verified. Create `YYYY-MM-DD-...md` and register in index. | Normal feature development or planned test iterations. |
| **ADR ([`.ai/decisions/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/))** | New subsystem designs, AST structural changes, indexing models. | Routine bug fixes adhering to existing architecture. |
| **[`.ai/context/current-state.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/current-state.md)** | Implementation status changes, test metrics changes, new capabilities. | Changes that do not alter capability snapshot. |
| **[`.ai/project-state.yaml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/project-state.yaml)** | Passing test counts, test suite registrations, ADR registrations. | Tasks that do not change test metrics or architecture. |
| **[`.ai/handoff.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/handoff.md)** | Meaningful progress, active task state, discoveries, next priorities. | Trivial queries where no state changed. |
| **[`AGENTS.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/AGENTS.md)** | Permanent instructions, invariants, or tool priorities. | Transient state or feature progress. |

---

## 6. Phase 8 & 9: Update & Anti-Assumption Re-Read

### Anti-Assumption Verification Rule
Before proceeding to commits, the agent **MUST re-read every modified documentation or state file** using `client_view_file` to confirm:
1. Edits are present, syntactically valid, and complete.
2. No existing sections were accidentally dropped or truncated.
3. Test counts, versions, and cross-references are mutually consistent.
4. User changelog contains zero technical jargon.

---

## 7. Mandatory Final Task Gate & Completion Table

```text
FINAL TASK GATE CHECKLIST:
[ ] 1. Inspect implementation diff (changes match requirements).
[ ] 2. Run verification harness (scripts/verify-patch.ps1 passes all gates).
[ ] 3. Post-edit inspection clean (get_file_problems and lint_files report 0 errors, 0 warnings, 0 weak warnings).
[ ] 4. Determine affected documentation/state files (via Decision Rules).
[ ] 5. Update every applicable documentation/state file (including .ai/bugs/ for bug fixes).
[ ] 6. Anti-assumption re-read executed on all modified files.
[ ] 7. Commit changes on task branch with conventional commit messages.
[ ] 8. Merge into master (--ff-only) and re-verify master.
[ ] 9. Delete temporary task branch.
[ ] 10. Explicitly report the status of each artifact in the Final Artifact Completion Table.
```

### Mandatory Final Artifact Completion Table

Every task completion report **MUST** include this table:

| Artifact | Status | Details / Justification |
|:---|:---|:---|
| **Task Branch & Git Session** | `Completed` / `Preserved (Incomplete)` / `N/A (Read-only)` | [Branch name, merge status, cleanup status] |
| **Deterministic Verification** | `PASSED` / `FAILED` | [`build/verification-report.json` overall status & passed gates] |
| **MCP Inspection** | `Clean (0 issues)` / `N/A` | [Output of get_file_problems & lint_files] |
| **`CHANGELOG.md`** | `Updated (Unreleased)` / `Cleaned (Release)` / `N/A` | [User-facing summary or reason N/A] |
| **Bug Record (`.ai/bugs/`)** | `Created` / `N/A` | [Link to bug record and index entry, or reason N/A] |
| **ADR (`.ai/decisions/`)** | `Created` / `Updated` / `N/A` | [ADR-XXX or reason N/A] |
| **`current-state.md`** | `Updated` / `N/A` | [Summary or reason N/A] |
| **`project-state.yaml`** | `Updated` / `N/A` | [Metrics updated or reason N/A] |
| **`handoff.md`** | `Updated` / `N/A` | [Handoff summary or reason N/A] |
| **`AGENTS.md`** | `Updated` / `N/A` | [Rule change or reason N/A] |
