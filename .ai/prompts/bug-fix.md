# Executable Protocol: Bug Investigation, Resolution & Knowledge Escalation

This document defines the mandatory, deterministic execution protocol for diagnosing, fixing, testing, verifying, and documenting defects in `midnight-compact-plugin`.

---

## 1. Preconditions

Before executing any bug fix actions, the AI agent **MUST** verify:
1. `git status` is clean on `master` (no unstaged or uncommitted user edits).
2. Read and strictly follow all contracts in [`.agents/rules/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.agents/rules/):
   - [`architecture.rules.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.agents/rules/architecture.rules.md) (Layer isolation, generalization, $\le$ 400-line limit)
   - [`threading.rules.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.agents/rules/threading.rules.md) (PSI Read vs Write, background vs EDT)
   - [`psi-parser.rules.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.agents/rules/psi-parser.rules.md) (AST resilience, token advancement, namespace separation)
   - [`modern-java25.rules.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.agents/rules/modern-java25.rules.md) (Java 25 records, pattern matching, sequenced collections)
   - [`inspections-annotators.rules.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.agents/rules/inspections-annotators.rules.md) (3-phase annotator lifecycle, quick-fix previews)
   - [`toolchain-wsl.rules.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.agents/rules/toolchain-wsl.rules.md) (WSL translation, timeout sandboxing)
3. The IntelliJ project is open and the `idea` MCP server is responsive.
4. Gradle wrapper (`.\gradlew.bat` / `./gradlew`) and JDK 25 are available.

---

## 2. Sequential Steps (The 13-Step Protocol)

The agent must execute these 13 steps in exact chronological order without skipping any step:

```text
[Step 1: Workspace Isolation] ─── Create dedicated branch ai/fix-<slug>
        │
        ▼
[Step 2: Pre-Debug & Rules Load]  Read .agents/rules/ & query .ai/bugs/
        │
        ▼
[Step 3: Reproduction & Mirror Test] Write failing stdlib + user-defined mirror tests
        │
        ▼
[Step 4: Architectural Root Cause] Trace defect to root semantic layer (Type/Scope/UI)
        │
        ▼
[Step 5: Modular Java 25 Fix] ─── Fix at root layer; enforce <= 400-line limits & records
        │
        ▼
[Step 6: Post-Edit Inspection] ── get_file_problems & lint_files via MCP
        │
        ▼
[Step 7: Test & Arch Verification] scripts/verify-patch.ps1 & CompactArchitectureTest
        │
        ▼
[Step 8: Bug Knowledge Record] ── Create .ai/bugs/YYYY-MM-DD-...md & update index
        │
        ▼
[Step 9: Pattern Escalation] ─── Check .ai/bugs/recurring-patterns.md ladder
        │
        ▼
[Step 10: User Changelog] ────── Update CHANGELOG.md under ## [Unreleased] -> ### Fixed
        │
        ▼
[Step 11: Atomic Commits] ────── Stage & commit fix, docs, and changelog
        │
        ▼
[Step 12: Merge to Master] ───── Checkout master, merge --ff-only, run verify-patch.ps1
        │
        ▼
[Step 13: Cleanup & Artifact] ── Delete task branch, emit Final Completion Table
```

### Step 1: Workspace Isolation
- Inspect `git status`.
- Create and switch to a dedicated in-workspace task branch:
  ```bash
  git checkout -b ai/fix-<defect-slug>
  ```
  *(Never edit production files directly on `master`.)*

### Step 2: Pre-Debug Search & Rules Context Loading
- Load and adhere to all active contracts in [`.agents/rules/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.agents/rules/).
- Search [`.ai/bugs/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/bugs/) and [`.ai/bugs/recurring-patterns.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/bugs/recurring-patterns.md) using `grep_search` or `find_by_name`.
- Search terms must include: affected class name, subsystem, error message/symptom.
- Record whether matching or related patterns were found.

### Step 3: Reproduction Test & "User-Defined Mirror" Test
- Before editing production code, write a minimal, targeted failing test in `src/test/java/dev/verloren/midnight/...`.
- **Mandatory Generalization Guard**: If the defect involves a standard library construct (`Either`, `Maybe`, `Vector`, `default`), write a companion **User-Defined Mirror Test** exercising identical behavior on a user-defined generic struct (`Result<TVal, TErr>`, `Pair<A, B>`).
- Execute the test:
  ```powershell
  powershell -ExecutionPolicy Bypass -File .\scripts\verify-patch.ps1 -TestPattern "<FullClassName>.<testMethodName>"
  ```
- Confirm that the test fails with the reported defect symptom.

### Step 4: Architectural Root Cause Investigation
- Trace the defect to its **Root Semantic Layer**:
  - *Type System defect* (`dev.verloren.midnight.type`) -> Fix in type system; **NEVER** patch around type issues in `completion/` or `editor/`.
  - *Scope / Symbol Resolution defect* (`dev.verloren.midnight.resolve`) -> Fix in resolver.
  - *Presentation / Completion UI defect* (`dev.verloren.midnight.completion`) -> Fix in completion provider.
- Cite affected production class, lines, and upstream Compact compiler ground truth (`compact/compiler/parser.ss`, `lexer.ss`, `langs.ss`) or reference plugins (`intellij-rust`, `intellij-solidity`, `intellij-scala`).

### Step 5: Modular Implementation (Modern Java 25)
- Apply the minimal surgical fix in `src/main/java/dev/verloren/midnight/...`.
- Adhere strictly to [`.agents/rules/architecture.rules.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.agents/rules/architecture.rules.md) and all path-scoped rules in [`.agents/rules/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.agents/rules/):
  - **Modularity**: File length must remain $\le$ 400 lines; method length $\le$ 40 lines. Decompose large classes into dedicated providers/visitors.
  - **Zero Stdlib Special-Casing**: Never hardcode type names (`"Either"`, `"Maybe"`) in general resolvers or providers.
  - **Java 25 Standards**: Records for DTOs and cache keys; sequenced collections (`getFirst()`, `getLast()`); pattern matching switch expressions; unnamed variables `_`.
  - **Threading**: `ReadAction` for PSI reads; EDT `WriteCommandAction` for PSI writes; zero blocking `waitFor()` on EDT.
  - **AST resilience**: Guard against `null` and `PsiErrorElement`. Loops must advance lexer/builder.

### Step 6: Post-Edit Inspection Loop via MCP
- Immediately after editing each file, run IntelliJ MCP inspection:
  ```text
  execute_tool get_file_problems --filePath "<absolute-path>"
  execute_tool lint_files --files [\"<absolute-path>\"]
  ```
- Verify 0 errors, 0 warnings, 0 weak warnings, and 0 unapplied language modernization hints. Fix any flagged issues immediately.

### Step 7: Test & Architectural Verification
- Run the automated verification harness including architecture compliance:
  ```powershell
  powershell -ExecutionPolicy Bypass -File .\scripts\verify-patch.ps1 -TestPattern "<FullClassName>"
  ```
- Verify that:
  - Gate 1 (Compilation): SUCCESS.
  - Gate 2 (Plugin Structure): SUCCESS.
  - Gate 3 (Tests): Both reproduction test and companion User-Defined Mirror test PASS with 0 failures.
  - Machine-readable report [`build/verification-report.json`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/build/verification-report.json) is generated with status `"PASSED"`.

### Step 8: Bug Knowledge Base Record
- Write a permanent record to `.ai/bugs/YYYY-MM-DD-<feature>-<title>.md` following the standard schema in [`.ai/bugs/README.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/bugs/README.md):
  1. Symptoms & Failure Behavior
  2. Root Cause Analysis (citing code lines & compiler ground truth)
  3. Investigation & Evaluated Approaches
  4. Solution & Implementation
  5. Verification & Tests Added
  6. Prevention & Key Lessons Learned
- Register the new record in the chronological table in [`.ai/bugs/README.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/bugs/README.md).

### Step 9: Pattern Escalation Check
- Inspect [`.ai/bugs/recurring-patterns.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/bugs/recurring-patterns.md):
  - If 1st occurrence $\to$ Documented in bug record.
  - If 2nd occurrence of similar mechanism $\to$ Register as *Emerging Pattern* in `recurring-patterns.md`.
  - If $\ge 3$ occurrences $\to$ Escalate to a mandatory path rule in [`.agents/rules/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.agents/rules/) or [`AGENTS.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/AGENTS.md).

### Step 10: User Changelog Translation
- Add a user-facing entry under `## [Unreleased]` -> `### Fixed` in [`CHANGELOG.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/CHANGELOG.md).
- Strict Rule: Answer *"What changed for me?"*. Zero class names, method signatures, test counts, or internal AST jargon.

### Step 11: Atomic Commits on Task Branch
- Stage and commit changes atomically:
  ```bash
  git add src/ && git commit -m "fix(<subsystem>): <concise descriptive message>"
  git add .ai/bugs/ && git commit -m "docs(bugs): record <defect> root cause and resolution in knowledge base"
  git add CHANGELOG.md && git commit -m "docs(changelog): note <defect> fix in unreleased notes"
  ```

### Step 12: Merge to Master & Post-Merge Verification
- Switch to master and merge:
  ```bash
  git checkout master
  git merge --ff-only ai/fix-<defect-slug>
  ```
- Re-run verification directly on `master`:
  ```powershell
  powershell -ExecutionPolicy Bypass -File .\scripts\verify-patch.ps1 -Quick
  ```

### Step 13: Cleanup & Final Artifact Emission
- Delete temporary task branch:
  ```bash
  git branch -d ai/fix-<defect-slug>
  ```
- Verify `git status` is clean on `master`.
- Emit the Final Artifact Completion Table.

---

## 3. Mandatory Gates & Evidence Requirements

An AI agent **CANNOT** declare completion without producing the following verifiable evidence:

| Gate | Requirement | Mandatory Evidence in Agent Output |
|:---|:---|:---|
| **Gate 0: Isolation** | Branch created from master | Output of `git rev-parse --abbrev-ref HEAD` showing `ai/fix-...` |
| **Gate 1: Pre-Debug Search** | Query executed in `.ai/bugs/` | Explicit statement of search query and citing matching bug ID or confirming `0 prior matching records found` |
| **Gate 2: Reproduction** | Failing test executed before fix | Exact test method name and copy of failure assertion output |
| **Gate 3: Code & Threading** | Java 25 & path-scoped rules | Summary of records, sequenced collections, and `ReadAction`/`WriteCommandAction` guards used |
| **Gate 4: MCP Inspection** | 0 errors, 0 warnings | Tool output of `get_file_problems` (`errors: []`) and `lint_files` (`items: []`) |
| **Gate 5: Verification Harness** | All gates pass | Contents of [`build/verification-report.json`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/build/verification-report.json) (`overall_status: "PASSED"`) |
| **Gate 6: Bug Record** | File created & indexed | Path to `.ai/bugs/YYYY-MM-DD-...md` and updated line in `.ai/bugs/README.md` |
| **Gate 7: Changelog** | User-facing entry | Quoted user note under `## [Unreleased]` in `CHANGELOG.md` |
| **Gate 8: Post-Merge Check** | Master verified | Exit code 0 from post-merge verification run on `master` |

---

## 4. Failure Conditions

The workflow **MUST HALT IMMEDIATELY** if any of the following occur:
1. `git status` on `master` has dirty, uncommitted user changes before starting.
2. The reproduction test cannot be made to fail (indicates invalid reproduction assumptions).
3. `get_file_problems` or `lint_files` returns unresolved compiler errors or warnings after editing.
4. `scripts/verify-patch.ps1` returns exit code 1 or fails any gate.
5. Merge conflict occurs when merging to `master`.

---

## 5. Recovery Rules

- **If Reproduction Test Passes Unexpectedly**: Stop. Re-read bug report, inspect compiler ground truth, and adjust the test input to reflect the exact reported AST or runtime conditions.
- **If MCP Inspection Flags Warnings**: Do NOT ignore them. Apply the requested Java 25 idiom or fix the warning immediately before running test suites.
- **If `verify-patch.ps1` Fails**: Inspect `build/verification-report.json`, fix the failing test or compilation error, and re-run until status is `"PASSED"`.
- **If Merge to Master Fails**: Do NOT force merge. Check git log, resolve branch divergence cleanly, or abort merge and report blocker.

---

## 6. Completion Conditions

The task is complete **ONLY** when:
1. Production code is fixed with 0 compiler warnings and 0 inspection items.
2. The reproduction test passes reliably.
3. Bug record is written and indexed in `.ai/bugs/`.
4. `CHANGELOG.md` is updated.
5. All changes are committed and fast-forward merged to `master`.
6. `master` branch verification passes.
7. Temporary task branch is deleted.
8. The Final Artifact Completion Table is presented with every artifact accounted for.
