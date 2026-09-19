# Executable Protocol: Feature Planning, Implementation & State Synchronization

This document defines the mandatory, deterministic execution protocol for planning, implementing, testing, verifying, and documenting new features, completions, live templates, intentions, and toolchains in `midnight-compact-plugin`.

---

## 1. Preconditions

Before executing any feature implementation actions, the AI agent **MUST** verify:
1. `git status` is clean on `master` (no unstaged or uncommitted user edits).
2. The feature requirements are grounded in Compact compiler sources (`compact/compiler/`) or language specifications.
3. The IntelliJ project is open and the `idea` MCP server is responsive.
4. Gradle wrapper (`.\gradlew.bat` / `./gradlew`) and JDK 25 are available.

---

## 2. Sequential Steps

The agent must execute these 8 steps in exact chronological order:

```text
[Step 1: Workspace Isolation] ─── Create dedicated branch ai/<feature-slug>
        │
        ▼
[Step 2: Ground Truth & ADR] ─── Inspect compact/compiler/ & existing ADRs (001-034)
        │
        ▼
[Step 3: Java 25 Implementation] Modern Java 25, PSI/threading invariants, records
        │
        ▼
[Step 4: Multi-Tier Testing] ──── Golden AST, error recovery, semantic resolve tests
        │
        ▼
[Step 5: Post-Edit Inspection] ─ get_file_problems & lint_files via MCP
        │
        ▼
[Step 6: Test Verification] ─── scripts/verify-patch.ps1 (all gates pass)
        │
        ▼
[Step 7: State Sync & Changelog] Update project-state.yaml, current-state.md, CHANGELOG.md
        │
        ▼
[Step 8: Merge, Master Check & Cleanup] ff-merge to master, verify, delete branch
```

### Step 1: Workspace Isolation
- Inspect `git status`.
- Create and switch to a dedicated in-workspace task branch:
  ```bash
  git checkout -b ai/<feature-slug>
  ```
  *(Never edit production files directly on `master`.)*

### Step 2: Ground Truth & Architectural Alignment
- Ground the feature in authoritative sources:
  - Official Compact compiler source in `compact/compiler/` (e.g. `parser.ss`, `standard-library.compact`, or `.ai/context/compact-semantics.md`).
  - Production reference plugins (`intellij-rust`, `intellij-scala`, `intellij-elixir`, `Rplugin`).
- Review existing ADRs in [`.ai/decisions/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/):
  - If the feature introduces a new subsystem, non-trivial AST restructure, or stub indexing model, draft an ADR first via [`.ai/prompts/architecture-decision.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/architecture-decision.md).

### Step 3: Implementation (Modern Java 25)
- Implement production code in `src/main/java/dev/verloren/midnight/...`.
- Adhere to path-scoped rules in [`.agents/rules/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.agents/rules/):
  - Records (`record`) for immutable data structures, AST node pairs, resolver cache keys, and DTOs.
  - Sequenced Collections (`getFirst()`, `getLast()`, `reversed()`). Forbidden: legacy `get(0)`.
  - Pattern matching switch expressions with arrow syntax (`->`) and record deconstruction patterns.
  - Unnamed variables and patterns (`_`) for unused arguments or exceptions.
  - Immutable collection factories (`List.of()`, `Set.of()`, `Map.of()`).
  - Multi-line text blocks (`"""..."""`) for templates, messages, and snippets.
  - Threading: PSI reads inside `ReadAction`; PSI mutations on EDT inside `WriteCommandAction`; zero `process.waitFor()` on EDT.
  - AST resilience: Guard against `null` and `PsiErrorElement`. Loops must advance tokens.

### Step 4: Multi-Tier Test Implementation
- Implement comprehensive automated tests in `src/test/java/dev/verloren/midnight/...`:
  - **Tier 1**: Golden AST tree tests (`.compact` -> `.txt`) asserting structure conformance.
  - **Tier 2**: Incomplete/broken code recovery (deliberately malformed syntax must produce `PsiErrorElement` without throwing exceptions or freezing editor).
  - **Tier 3**: Semantic resolution, type checking, or scope visibility (`VALUE` vs `TYPE` namespace separation).
  - **Tier 4**: Concurrency, cancellation, and stress resilience.

### Step 5: Continuous Post-Edit Inspection Loop via MCP
- Immediately after EVERY file edit, run IntelliJ inspections via MCP:
  ```text
  execute_tool get_file_problems --filePath "<absolute-path>"
  execute_tool lint_files --files [\"<absolute-path>\"]
  ```
- Strict Zero-Tolerance: 0 errors (`ERROR`), 0 warnings (`WARNING`), 0 weak warnings (`WEAK WARNING`), 0 grammar/spelling errors, and all modern Java 25 suggestions applied.
- Fix all detected issues immediately before moving to the next edit.

### Step 6: Multi-Gate Verification Harness
- Run the automated verification harness:
  ```powershell
  powershell -ExecutionPolicy Bypass -File .\scripts\verify-patch.ps1 -TestPattern "<NewFeatureTestClass>"
  ```
- Verify:
  - Gate 1 (Java 25 Compilation): SUCCESS.
  - Gate 2 (Plugin Structure): SUCCESS (validates `plugin.xml` extension points and template bindings).
  - Gate 3 (Tests): 100% pass rate with zero failures.
  - Generated [`build/verification-report.json`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/build/verification-report.json) has status `"PASSED"`.

### Step 7: State Synchronization & Clean Changelog
- Update [`.ai/project-state.yaml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/project-state.yaml):
  - Update passing test counts, test suites list, and phase statuses.
- Update [`.ai/context/current-state.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/current-state.md):
  - Update capability snapshot, completed phase status, and test metrics table.
- Update [`CHANGELOG.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/CHANGELOG.md) under `## [Unreleased]` -> `### Added` or `### Changed`:
  - Write clean, minimal, human-centric user notes answering *"What changed for me?"*.
  - Strict Rule: NEVER include internal class names, method signatures, test counts, or ADR numbers.
- Update [`.ai/handoff.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/handoff.md) with session accomplishments and next priorities.

### Step 8: Atomic Commits, Merge to Master, & Cleanup
- Stage and commit changes atomically:
  ```bash
  git add src/ && git commit -m "feat(<subsystem>): <concise descriptive message>"
  git add .ai/ CHANGELOG.md && git commit -m "docs(context): record <feature> implementation and update test metrics"
  ```
- Switch to `master` and merge:
  ```bash
  git checkout master
  git merge --ff-only ai/<feature-slug>
  ```
- Re-run verification directly on `master`:
  ```powershell
  powershell -ExecutionPolicy Bypass -File .\scripts\verify-patch.ps1 -Quick
  ```
- Delete temporary task branch:
  ```bash
  git branch -d ai/<feature-slug>
  ```
- Verify `git status` is clean on `master` and emit Final Artifact Completion Table.

---

## 3. Mandatory Gates & Evidence Requirements

| Gate | Requirement | Mandatory Evidence in Agent Output |
|:---|:---|:---|
| **Gate 0: Isolation** | Branch created from master | Output of `git rev-parse --abbrev-ref HEAD` showing `ai/<feature-slug>` |
| **Gate 1: Specification** | Ground truth cited | Source file in `compact/compiler/` or reference plugin cited |
| **Gate 2: Java 25 & Invariants** | Clean modern implementation | Summary of records, sequenced collections, and threading guards used |
| **Gate 3: Multi-Tier Tests** | Tiers 1-3 tests added | Test class names and count of added test methods |
| **Gate 4: MCP Inspection** | 0 errors, 0 warnings | Tool output of `get_file_problems` (`errors: []`) and `lint_files` (`items: []`) |
| **Gate 5: Verification Harness** | All gates pass | Contents of [`build/verification-report.json`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/build/verification-report.json) (`overall_status: "PASSED"`) |
| **Gate 6: State Sync** | YAML & Markdown updated | Updated test count in `.ai/project-state.yaml` and `current-state.md` |
| **Gate 7: Changelog** | User-facing entry | Quoted user note under `## [Unreleased]` in `CHANGELOG.md` |
| **Gate 8: Post-Merge Check** | Master verified | Exit code 0 from post-merge verification run on `master` |

---

## 4. Failure Conditions

Execution **MUST HALT** if:
1. `git status` on `master` contains uncommitted user changes.
2. The language feature contradicts Compact grammar ground truth.
3. Code fails Java 25 compilation or produces compiler warnings.
4. `verifyPluginStructure` detects invalid XML or missing template attributes.
5. Unit tests fail or cause timeouts/freezes.
6. Merge to `master` cannot be performed cleanly.

---

## 5. Recovery Rules

- **If Compilation Fails**: Diagnose compiler error log, verify Java 25 syntax, and re-compile.
- **If Plugin Structure Fails**: Inspect `plugin.xml`, ensure extension points (e.g. `completion.contributor`, `annotator`) have valid `implementationClass` and `language="Compact"`.
- **If Tests Fail**: Check test fixture input against golden syntax trees in `src/test/testData/`. Fix PSI tree creation or resolve logic.
- **If MCP Reports Warnings**: Immediately refactor code to eliminate warnings before proceeding to verification.

---

## 6. Completion Conditions

The task is complete **ONLY** when:
1. All feature code and multi-tier tests are implemented.
2. MCP inspection reports 0 errors, 0 warnings, and 0 weak warnings.
3. `scripts/verify-patch.ps1` reports `overall_status: "PASSED"`.
4. `project-state.yaml`, `current-state.md`, `handoff.md`, and `CHANGELOG.md` are synchronized.
5. Code is cleanly merged into `master` and re-verified.
6. Temporary branch is deleted and working tree is clean.
7. Final Artifact Completion Table is presented.
