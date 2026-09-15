# Task Lifecycle, Workflows, & Completion Gate

This document defines the strict, standard execution lifecycle for any software development, refactoring, bug-fixing, code review, release-time changelog cleanup, and release task within `midnight-compact-plugin`.\n
---

## 1. Task Lifecycle Overview

Every development session modifying repository files must progress sequentially through this isolated worktree lifecycle:

```text
SESSION START
  │
  ▼
[0. Git Worktree Setup] ────────── Inspect git state, verify master, create dedicated worktree & branch
  │
  ▼
[1. Context Loading & Pre-Debug] ── Load AGENTS.md + task-specific files; search .ai/bugs/ if fixing defects
  │
  ▼
[2. Inspect Implementation] ────── Read code, tests, compiler refs, & reference plugins inside worktree
  │
  ▼
[3. Plan & Align] ──────────────── Check invariants, consult ADRs, formulate minimal plan
  │
  ▼
[4. Implement] ────────────────── Write modern Java (Java 14–25) inside worktree adhering to threading & PSI models
  │
  ▼
[5. Test & Validate] ──────────── Continuous inspection after EVERY edit (0 errors/warnings/weak warnings), multi-tier testing
  │
  ▼
[6. Code Review Self-Check] ────── Verify against Section 9 anti-bikeshedding & invariant bar
  │
  ▼
[7. Documentation Impact] ──────── Apply Intelligent Documentation Decision Rules (including .ai/bugs/)
  │
  ▼
[8. Update Documentation] ──────── Make targeted updates to applicable doc/state/bug files inside worktree
  │
  ▼
[9. Verify Documentation] ──────── Re-read modified files (anti-assumption rule); confirm content & integrity
  │
  ▼
[10. Final Worktree Verification] ─ Re-run diff check & test suites (zero regressions) inside worktree
  │
  ▼
[11. Commit Task Branch] ───────── Commit all verified changes to ai/<task-slug>
  │
  ▼
[12. Merge into Master & Verify] ── Switch to master checkout, merge task branch, run test suite on master
  │
  ▼
[13. Cleanup & Final Gate] ─────── Delete worktree, delete temporary branch, execute Mandatory Gates
  │
  ▼
SESSION COMPLETE
```

When preparing a release or publishing a release tag, the lifecycle transitions into the **Release & Publication Protocol (Section 8)**:

```text
FINISH IMPLEMENTATION
  │
  ▼
Update Unreleased in CHANGELOG.md during development
  │
  ▼
PREPARE RELEASE
  │
  ▼
Review and clean CHANGELOG.md (User-facing rewrite & consolidation)
  │
  ▼
Convert Unreleased into user-facing release notes (## [X.Y.Z] - YYYY-MM-DD)
  │
  ▼
Update version/date (gradle.properties)
  │
  ▼
Update release metadata (plugin.xml fallback change-notes & version)
  │
  ▼
Run final verification (./gradlew test & ./gradlew buildPlugin)
  │
  ▼
Commit release changes on master
  │
  ▼
Create / push release tag (ONLY when explicitly requested)
```

---

## 2. Mandatory Git Worktree Session Lifecycle

### 2.1 Core Principle & Purpose
To prevent unfinished work, experimental code, broken intermediate compilation states, and unrelated modifications from polluting `master`, **every development session that modifies repository files MUST operate inside a dedicated, isolated Git worktree**.

`master` is the pristine, releasable baseline. Work is performed in isolation and merged back only when 100% complete, fully tested, documented, and verified.

> [!IMPORTANT]
> **Read-Only Exception**: Purely read-only tasks (e.g., answering questions, code navigation, architectural lookups, or review inquiries that produce zero file changes) are exempt from creating a worktree. The worktree lifecycle is mandatory for any session that makes repository edits.

### 2.2 Pre-Flight Inspection & Safe Handling of Existing Changes
Before creating a worktree or editing files, the agent MUST inspect the existing environment:
1. **Handle Existing User Changes Safely**:
   - Inspect `git status`, `git diff`, and `git status -s` on `master`.
   - **NEVER silently discard, reset, stash destructively, overwrite, or revert existing user modifications on `master`**.
   - The AI must NEVER execute destructive commands such as `git reset --hard` or `git clean -fd` merely to make worktree creation succeed.
   - If uncommitted changes exist on `master`, either leave them uncommitted on `master` if unrelated, or if they are relevant, ask the user or document them before proceeding.
2. **Inspect Existing Worktrees**:
   - Run `git worktree list` to see all active worktrees.
   - Run `git branch` to inspect existing branches.
   - Check [`.ai/handoff.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/handoff.md) to see if a previous session left an incomplete task or active worktree.

### 2.3 Session Identity & Naming Standard
Every modifying session must establish a distinct identity based on the task:
- **Worktree Directory**: `../<project>-wt-<task-slug>` (e.g., `../midnight-plugin-wt-default-template-naming`, `../midnight-plugin-wt-parser-recovery`).
  - Located as a sibling directory to the project repository root.
- **Dedicated Branch**: `ai/<task-slug>` (e.g., `ai/default-template-naming`, `ai/parser-recovery`).
  - Derived cleanly from `master`.
- **Prohibited Generic Names**: The following names are **strictly forbidden** for branches or worktree paths:
  `test`, `temp`, `branch1`, `work`, `ai-work`, `task`, `patch`, `dev`.

### 2.4 Worktree Creation & Working Context Switch
1. Create the branch and worktree in a single command:
   ```bash
   git worktree add -b ai/<task-slug> ../<project>-wt-<task-slug> master
   ```
2. **Context Switching**:
   - All tool commands that support a working directory (e.g. `run_command` with `Cwd`) MUST be pointed to the newly created worktree directory `../<project>-wt-<task-slug>`.
   - File editing and viewing tools must use paths targeting files within that worktree.
   - The agent must explicitly confirm it is working within the worktree path before creating or modifying files.

### 2.5 Work Exclusively in the Worktree
Once the worktree is established:
- **ALL** code implementations, refactorings, test fixtures, and test runs must occur inside `../<project>-wt-<task-slug>`.
- **ALL** documentation updates (`.ai/*`, `CHANGELOG.md`, ADRs, bug records, `AGENTS.md`) must occur inside `../<project>-wt-<task-slug>`.
- The agent must NEVER make task modifications directly on `master`.

### 2.6 Task Execution Inside Worktree (Phases 1–9)
Inside the worktree, the agent executes:
1. Context loading & pre-debug search in `.ai/bugs/`.
2. Inspect implementation and compiler references.
3. Plan and architectural alignment.
4. Comprehensive modern Java (Java 14–25) implementation conforming to threading and PSI invariants.
5. Multi-tier testing and validation (`./gradlew test`) with continuous post-edit inspection (`get_file_problems`, `lint_files`).
6. Code review self-check against the anti-bikeshedding bar.
7. Documentation impact analysis via Decision Rules.
8. Apply targeted documentation updates (including bug records in `.ai/bugs/` if fixing a bug).
9. Verify documentation with the anti-assumption re-read rule.

### 2.7 Commit on Task Branch Before Merge
Before merging into `master`:
1. Verify `git status` inside the worktree shows all modified files are accounted for and no unwanted temporary files exist.
2. Run `./gradlew test` inside the worktree to ensure 100% clean pass.
3. Stage and commit all changes cleanly on `ai/<task-slug>`:
   ```bash
   git add .
   git commit -m "<type>(<scope>): <clear descriptive summary of changes>"
   ```
4. Confirm `git status` is clean on `ai/<task-slug>`.

### 2.8 Non-Destructive Merge into Master
After committing on the task branch:
1. Switch context back to the primary repository checkout (`master`).
2. Verify `master` is clean or contains only pre-existing untouched user work.
3. Merge the task branch into `master`:
   ```bash
   git merge ai/<task-slug>
   ```
4. Never force-push or rebase destructively over user commits.

### 2.9 Post-Merge Master Verification
Immediately after merging into `master`:
1. Verify `git log -n 1` shows the newly merged commit on `master`.
2. Verify `git status` on `master` is healthy.
3. Run `./gradlew test` on `master` to confirm the merged repository builds and passes all tests with zero failures and zero warnings.

### 2.10 Worktree and Branch Cleanup
Only after `master` is verified:
1. Remove the worktree:
   ```bash
   git worktree remove ../<project>-wt-<task-slug>
   ```
2. Delete the temporary task branch:
   ```bash
   git branch -d ai/<task-slug>
   ```
3. Run `git worktree list` to confirm the worktree is gone and `master` is the active worktree.

> [!CAUTION]
> **Cleanup Safety Sequence**:
> - Never delete a branch before it has been successfully merged into `master`.
> - Never delete a worktree before `master` test verification passes.
> - If merge or verification fails, preserve the worktree for inspection.

### 2.11 Failure & Interrupted Session Handling
If a task cannot be completed, tests fail, or the session is interrupted before completion:
1. **DO NOT merge into `master`**.
2. **DO NOT delete the task branch**.
3. **DO NOT remove or delete the worktree**.
4. Leave the worktree in place and commit work-in-progress on `ai/<task-slug>` if helpful.
5. Immediately document the state in [`.ai/handoff.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/handoff.md) using this exact schema:
   ```markdown
   ## Interrupted Worktree Session
   - **Task**: <description of the task>
   - **Branch**: ai/<task-slug>
   - **Worktree**: ../<project>-wt-<task-slug>
   - **Completed**: <what has been done>
   - **Remaining**: <what remains to be done>
   - **Discoveries**: <important technical findings or roadblocks>
   - **Test Status**: <current test results / failing tests>
   - **Next Recommended Action**: <concrete next step for resuming agent>
   ```

### 2.12 Existing Worktrees & Continuation
Before creating any new worktree, agents MUST check:
1. Does `git worktree list` show an existing worktree for this task?
2. Does `[ .ai/handoff.md ]` indicate an unfinished session on this feature?
If an unfinished worktree already exists:
- Inspect its state (`git status`, `git log`, `handoff.md`).
- Resume work **inside the existing worktree** rather than creating a competing duplicate worktree.

---

## 3. Phase 1: Context Loading & Task Routing

Before proposing or editing anything, the agent **MUST** load the exact set of context files required for the task type:

| Task Type | Mandatory Reading Before Action |
|:---|:---|
| **Feature Planning** | 1. [`AGENTS.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/AGENTS.md)<br>2. [`.ai/project-state.yaml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/project-state.yaml)<br>3. [`.ai/context/current-state.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/current-state.md)<br>4. [`.ai/context/architecture.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/architecture.md)<br>5. Relevant ADRs in [`.ai/decisions/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/)<br>6. Targeted sections in [`.ai/context/compact-semantics.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/compact-semantics.md) |
| **Feature Implementation** | 1. [`AGENTS.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/AGENTS.md) (Modern Java & Threading rules)<br>2. Targeted production files & tests via MCP `search_symbol` / `get_symbol_info`<br>3. [`.ai/context/intellij-patterns.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/intellij-patterns.md)<br>4. Relevant local reference plugin (`intellij-rust`, `intellij-elixir`, `intellij-scala`, `Rplugin`) |
| **Bug Fixing** | 1. Prior bug records: Search [`.ai/bugs/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/bugs/) by subsystem, error, or symptom to check for existing solutions or related defects<br>2. Targeted failing test or fixture in `src/test/`<br>3. Targeted production implementation in `src/main/`<br>4. Official compiler ground truth (`compact/compiler/`) or [`.ai/context/compact-semantics.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/compact-semantics.md)<br>5. [`.ai/context/intellij-patterns.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/intellij-patterns.md) for platform nuances |
| **Architecture / Subsystem Change** | 1. [`AGENTS.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/AGENTS.md) (Invariants & Threading)<br>2. [`.ai/context/architecture.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/architecture.md)<br>3. Existing ADRs in [`.ai/decisions/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/) & [`.ai/decisions/README.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/README.md)<br>4. Upstream compiler sources (`compact/compiler/`) & reference plugins |
| **Code Review / Quality Audit** | 1. [`AGENTS.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/AGENTS.md) Section on Code Review (Anti-Bikeshedding Bar)<br>2. [`AGENTS.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/AGENTS.md) Section on Threading & Concurrency<br>3. Target files to review |
| **Release / Distribution Work** | 1. [`.ai/workflow.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/workflow.md) Section 8 (Release & Publication Protocol)<br>2. [`CHANGELOG.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/CHANGELOG.md)<br>3. [`gradle.properties`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/gradle.properties)<br>4. [`src/main/resources/META-INF/plugin.xml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/resources/META-INF/plugin.xml) |
| **Session Handoff & Continuation** | 1. [`.ai/handoff.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/handoff.md)<br>2. [`.ai/project-state.yaml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/project-state.yaml)<br>3. [`.ai/context/current-state.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/current-state.md) |

---

## 4. Phases 2–6: Execution & Verification

### Phase 2: Inspect Existing Implementation
- Never write code based on assumptions or generic knowledge.
- Read targeted production classes and existing test fixtures before proposing edits.
- **Pre-Debugging Search**: When fixing bugs or investigating failures, agents **MUST first search [`.ai/bugs/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/bugs/)** by feature name, subsystem, affected class, error message, or failure symptom. If a related bug exists, review its root cause and solution before re-investigating from scratch, but always verify root causes match rather than blindly copying code.
- If implementing language constructs or editor behaviors, inspect the corresponding ground truth in `compact/compiler/` and the appropriate reference plugin (`intellij-rust`, `intellij-scala`, `intellij-elixir`, `Rplugin`).

### Phase 3: Plan & Architectural Alignment
- Verify that the plan obeys all Critical Invariants in `AGENTS.md` (no replacing working architecture, reuse `CompactResolveUtil` / `CompactElementFactory`, strict namespace separation, incomplete PSI tolerance, Git worktree isolation, changelog release hygiene).
- Check whether an ADR already exists for this subsystem in `.ai/decisions/`.
- Ensure the plan avoids brittle heuristics and hardcoded patterns.

### Phase 4: Implementation
- **Full Spectrum Modern Java (Java 14–25)**:
  - Utilize the full range of modern Java features introduced through Java 25:
    - **Records (`record`)**: Mandatory for immutable data models, DTOs, AST containers, and resolver/cache keys. Explicitly avoid boilerplate POJOs with manual getters/setters.
    - **Sequenced Collections**: Mandatory usage of `getFirst()`, `getLast()`, `reversed()`, `addFirst()`, `removeFirst()`, and sequenced map/set views. Legacy `get(0)` and `get(size() - 1)` are strictly forbidden.
    - **Pattern Matching**: Switch expressions with arrow syntax, type patterns with `when` guards, and record deconstruction patterns (`instanceof Point(int x, int y)`). Legacy statement switches with colon and `break;` are forbidden.
    - **Unnamed Patterns & Variables (`_`)**: Mandatory use of `_` for unconsumed exception parameters (`catch (Exception _)`), pattern bindings (`case Type _`), and unused lambda parameters. Never declare unused named variables.
    - **Sealed Hierarchies**: Closed algebraic data types using `sealed` and `permits` for exhaustive switch expressions without redundant fallback branches.
    - **Modern Collections & Streams**: Immutable collection factories (`List.of()`, `Set.of()`, `Map.of()`), direct `.toList()` on streams, `Stream.ofNullable()`, `takeWhile()`, and `dropWhile()`.
    - **Text Blocks & Modern String APIs**: `"""` text blocks for multi-line strings, `isBlank()`, `strip()`, `repeat()`, and `formatted()`.
- **Strict Threading Rules**: PSI reads inside `ReadAction`, PSI mutations on EDT inside `WriteCommandAction`, long operations on background threads with cancellation support.
- **Robustness**: Guard against null and `PsiErrorElement` nodes. Never freeze the UI thread or cause `StackOverflowError`.

### Phase 5: Test & Validate
- **Mandatory Post-Edit Code Inspection (Every Edit)**:
  - Immediately following **EVERY SINGLE EDIT** to any file, the agent MUST run IntelliJ inspections via MCP `execute_tool`:
    - Live problem check: `get_file_problems --filePath <absolute_path>`
    - Static inspections & linter: `lint_files --files ["<absolute_path>"]`
  - **Zero Tolerance Policy**: Inspect for and immediately resolve ALL reported problems: errors, warnings, weak warnings, and bugs. Never proceed to subsequent edits or commits while any warning or weak warning remains.
- **Multi-Tier Testing Standard**:
  - **Tier 1**: Golden AST tree conformance tests.
  - **Tier 2**: Partial parsing and error recovery (deliberately broken syntax must produce `PsiErrorElement` without freezing or crashing).
  - **Tier 3**: Operator precedence and associativity.
  - **Tier 4**: Stress and recursion resilience.
  - **Tier 5**: Non-blocking concurrency and cancellation.
- Run `./gradlew test` inside the worktree and confirm 100% passing tests with zero failures and zero compiler warnings.

### Phase 6: Code Review Self-Check
- Evaluate changes against the anti-bikeshedding bar in `AGENTS.md`:
  - No cosmetic rewrites (no converting working loops to streams or reformatting untouched code).
  - No speculative abstractions (no premature interfaces, builders, or generic factories).
  - No subjective renames of identifiers that already follow project conventions).
  - Only introduce changes justified by threading safety, critical invariants, language semantics, or tangible bug fixes.

---

## 5. Phase 7: Intelligent Documentation Decision Rules

Do NOT blindly update every file for every change, and NEVER update `AGENTS.md` with transient project state. For every task, evaluate each documentation artifact against these exact criteria:

| Artifact | When to Update | When NOT to Update |
|:---|:---|:---|
| **[`CHANGELOG.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/CHANGELOG.md)** | **During Development**: Add notable user-visible features, templates, completion improvements, inspections, or bug fixes under `## [Unreleased]`, phrased from the user perspective answering *\"What changed for me?\"*<br>**At Release Time**: Mandatory release cleanup before tagging. Rewrite/consolidate `## [Unreleased]` into curated user notes under `## [X.Y.Z] - YYYY-MM-DD`, stripping all technical jargon, ADR IDs, class/test names, and internal AI notes. Open fresh `## [Unreleased]` for future work. | **Never during development**: Do NOT record internal refactorings, private test additions, ADR numbers, class/method names, line numbers, commit logs, or internal AI restructuring.<br>**Never at release time**: NEVER push a release tag while `CHANGELOG.md` still contains internal engineering notes. |
| **Bug Record ([`.ai/bugs/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/bugs/))** | Whenever a concrete defect or incorrect behavior was identified, investigated, fixed, and verified (meeting all 5 trigger conditions in [`.ai/bugs/README.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/bugs/README.md)). Create `YYYY-MM-DD-<feature>-<title>.md` and register in [`.ai/bugs/README.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/bugs/README.md). | Normal feature development, planned test-driven development iterations, speculative discussions, abandoned prototypes, or trivial typos. |
| **ADR ([`.ai/decisions/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/))** | Architectural decisions, new subsystem designs, formalized language rules, significant AST/PSI structural changes, or non-trivial completion/refactoring algorithms. Register in [`.ai/decisions/README.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/README.md). | Routine bug fixes adhering to an existing ADR, minor test additions, or documentation improvements. |
| **[`.ai/context/current-state.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/current-state.md)** | Implementation status changes, new phase completion, new subsystem capabilities, known limitations, or baseline test metrics updates. | Code changes that do not alter the overall capability snapshot or test baseline. |
| **[`.ai/project-state.yaml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/project-state.yaml)** | Machine-readable project phase status changes (e.g. `planned` -> `complete`), passing test counts, new test suite registrations, or new ADR registrations. | Tasks that do not change test suite metrics, phase completion status, or architecture registry. |
| **[`.ai/handoff.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/handoff.md)** | Any session that produces meaningful implementation progress, active worktree continuation state, architectural discoveries, unresolved blockers, or next-step priorities. | Trivial single-command query tasks where no work state changed. |
| **[`AGENTS.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/AGENTS.md)** | ONLY when permanent agent instructions, architectural invariants, tool priorities, coding conventions, or forbidden patterns themselves change. | NEVER update `AGENTS.md` for feature completions, test count bumps, or phase progression. |
| **[`plugin.xml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/resources/META-INF/plugin.xml)** | When new extensions (inspections, actions, intentions, file templates) are added, or when updating the user-facing `<description>` or release `<change-notes>`. | Internal code changes that don't add or alter IntelliJ extension points. |

---

## 6. Phase 8 & 9: Update & Mandatory Re-Read Verification

### Phase 8: Apply Updates
Apply the necessary modifications to the determined documentation and state files inside the worktree using the appropriate client tools.

### Phase 9: Verify Documentation Edits (Anti-Assumption Rule)
> [!CAUTION]
> **NEVER ASSUME A SUCCESSFUL EDIT TOOL CALL MEANS THE FILE IS CORRECT.**
> Tool edits can partially fail, truncate content, introduce malformed YAML/Markdown, or leave stale numbers behind.

Before moving to the final gate, the agent **MUST re-read every modified documentation or state file** using `client_view_file` or `view_file` and explicitly confirm:
1. The modified lines are present and syntactically correct.
2. No existing sections were accidentally dropped, duplicated, or truncated.
3. Test counts, version numbers, and file references are mutually consistent across all updated files.
4. Any newly created bug record adheres to the standard schema in [`.ai/bugs/README.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/bugs/README.md), contains no placeholder text, and is registered in the chronological index table in [`.ai/bugs/README.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/bugs/README.md).
5. Any modified `CHANGELOG.md` entry adheres to the user-facing standard and contains zero engineering jargon, ADR IDs, class names, file paths, or internal AI notes.

---

## 7. Phase 10 & 11: Mandatory Final Task Gate

Before claiming that any development task is complete, the agent **MUST** execute these checklists and gates in order:

```text
FINAL TASK GATE CHECKLIST:
[ ] 1. Inspect implementation diff (verify all changes match requirements).
[ ] 2. Run relevant tests (./gradlew test passes with 0 failures).
[ ] 3. Check for IDE/compiler problems (MCP get_file_problems and lint_files report 0 errors, 0 warnings, 0 weak warnings).
[ ] 4. Determine which documentation/state files are affected (via Decision Rules).
[ ] 5. Update every applicable documentation/state file (including .ai/bugs/ if a bug was resolved).
[ ] 6. Re-read every modified documentation/state file to verify edits landed correctly.
[ ] 7. Inspect final diff again (confirm no stray files or inadvertent edits).
[ ] 8. Run final verification/tests (confirm build succeeds with zero warnings).
[ ] 9. Execute Mandatory Git Session Completion Gate (commit, merge to master, verify master, cleanup).
[ ] 10. If preparing a release or publishing a tag: execute Release Changelog Review Gate & Checklist (Section 8).
[ ] 11. Explicitly report the status of each artifact in the Final Artifact Completion Table.
```

### Mandatory Git Session Completion Gate
For every development session that modifies repository files, verify these 9 questions before claiming completion:
1. Dedicated worktree created (`../<project>-wt-<task-slug>`)? *(Yes / N/A for read-only tasks)*
2. Dedicated branch created (`ai/<task-slug>`)? *(Yes / N/A)*
3. All work performed exclusively inside the worktree? *(Yes / N/A)*
4. Changes cleanly committed on task branch? *(Yes / N/A)*
5. Branch merged cleanly into `master`? *(Yes / N/A)*
6. `master` verified after merge (tests pass, files present, log confirmed)? *(Yes / N/A)*
7. Worktree removed and directory deleted? *(Yes / N/A)*
8. Temporary task branch deleted? *(Yes / N/A)*
9. `master` working tree clean and safe? *(Yes / N/A)*
*(If ANY answer to 1–9 is NO for a repository-modifying task, the session is NOT complete).*

### Mandatory Bug Completion Gate
If the current task involved investigating and resolving a bug or defect, answer these 8 questions before claiming completion:
1. Did the task involve investigating and fixing a bug? *(If YES, proceed; if NO, mark N/A).*
2. Was the root cause clearly identified and explained?
3. Has the fix been implemented and verified with automated tests?
4. Has a new bug record been written to `.ai/bugs/YYYY-MM-DD-<feature>-<title>.md`?
5. Does the record strictly follow the standard markdown schema in [`.ai/bugs/README.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/bugs/README.md)?
6. Does the record explain symptoms, root cause, investigation, solution, verification, and prevention?
7. Has [`.ai/bugs/README.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/bugs/README.md) index table been updated with a link to the new record?
8. Have all modified/created files been verified with the anti-assumption re-read rule?
*(If ANY answer to 2–8 is NO, the task is NOT done).*

### Mandatory Final Artifact Completion Table

Every task completion report **MUST** include this table with the status of each artifact:

| Artifact | Status | Details / Justification |
|:---|:---|:---|
| **Git Worktree Session** | `Completed` / `Preserved (Incomplete)` / `Not applicable (Read-only)` | [Branch name, worktree path, merge status, cleanup status, or reason why N/A] |
| **`CHANGELOG.md`** | `Updated (Unreleased)` / `Cleaned & Versioned (Release)` / `Not applicable` | [Specific section updated, cleaned for release, or reason why not applicable] |
| **Bug Record (`.ai/bugs/`)** | `Created` / `Not applicable` | [Link to `.ai/bugs/YYYY-MM-DD-...md` and entry in `.ai/bugs/README.md`, or reason why not applicable] |
| **ADR (`.ai/decisions/`)** | `Created` / `Updated` / `Not applicable` | [ADR-XXX or reason why not applicable] |
| **`current-state.md`** | `Updated` / `Not applicable` | [Status summary or reason why not applicable] |
| **`project-state.yaml`** | `Updated` / `Not applicable` | [Metrics updated or reason why not applicable] |
| **`handoff.md`** | `Updated` / `Not applicable` | [Session handoff summary or reason why not applicable] |
| **`AGENTS.md`** | `Updated` / `Not applicable` | [Rule change or reason why not applicable] |
| **Other Context / Config** | `Updated` / `Not applicable` | [Specify file or reason why not applicable] |

---

## 8. Release & Publication Protocol

This protocol governs the mandatory process for cleaning the changelog, preparing releases, updating metadata, and cutting releases.

### 8.1 Core Principle: The Public User-Facing Standard
This project is an IntelliJ IDEA plugin. [`CHANGELOG.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/CHANGELOG.md) is written **strictly for plugin users** and must be immediately understandable without any knowledge of the repository's internal architecture, classes, or AI workflows.

Every entry in the public release notes must answer:
> **\"What changed for me?\"**

Changelogs follow [Keep a Changelog](https://keepachangelog.com/): they are **curated for humans** and contain notable changes, rather than serving as a commit log or raw development history.

The AI documentation system establishes three distinct tiers of project knowledge:
1. **Development `CHANGELOG.md` (`## [Unreleased]`)**: The working collection point for notable user-facing changes during active development.
2. **Release `CHANGELOG.md` (`## [X.Y.Z] - YYYY-MM-DD`)**: Curated, consolidated, human-facing release notes stripped of all engineering jargon.
3. **Internal Engineering Knowledge (`.ai/`)**: The complete, permanent engineering history ([`.ai/bugs/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/bugs/), [`.ai/decisions/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/), [`.ai/context/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/), git commit history, and code documentation).

> [!IMPORTANT]
> **Preserve Engineering History Elsewhere**: Do NOT delete useful implementation knowledge merely because it is removed from the public changelog. Deep technical explanations, compiler citations, test names, and AST mechanics must remain fully documented in `.ai/`, ADRs, bug records, and git commits. The changelog is the **public summary**, not the engineering diary.

### 8.2 Mandatory Release Execution Order (The Tagging Guard)
Before creating or pushing any release tag, the AI MUST execute this exact sequential workflow:

```text
Finish implementation
    │
    ▼
Update Unreleased during development (notable changes only)
    │
    ▼
Prepare release
    │
    ▼
Review and clean CHANGELOG.md (User-Facing Changelog Standard)
    │
    ▼
Convert Unreleased into user-facing release notes
    │
    ▼
Update version and date (gradle.properties)
    │
    ▼
Update release metadata (plugin.xml fallback change-notes & version)
    │
    ▼
Run final verification (./gradlew test & ./gradlew buildPlugin)
    │
    ▼
Commit release changes on master
    │
    ▼
Create / push release tag (ONLY when explicitly requested)
```

> [!CAUTION]
> **THE TAGGING GUARD**:
> - **NEVER push or create a release tag while `CHANGELOG.md` still contains internal AI/developer notes intended only for engineering history.**
> - **DO NOT automatically create or push release tags merely because implementation is finished.** Tags must only be created or pushed when the release workflow explicitly calls for it or the user explicitly commands it.

### 8.3 What Must Be Removed or Translated (The Technical Blacklist)
During release cleanup, inspect every entry under `## [Unreleased]`. The following internal details **MUST be removed or translated into their user-facing effect**:

| Category | Forbidden in Release Changelog | Correct User-Facing Translation |
|:---|:---|:---|
| **ADR Identifiers** | `ADR-026`, `ADR-028`, `ADR-019` | Describe the user capability directly (e.g., \"Added support for Compact export declarations\"). |
| **Phase Numbers** | `Phase 29`, `Phase 28` | Omit completely; describe the feature functionality. |
| **Java Class Names** | `CompactCompletionContributor`, `CompactDeclarationNameGenerator` | \"code completion\", \"automatic declaration numbering\". |
| **Method Names** | `isExportable()`, `registerCustomType()` | \"export validation\", \"declaration registry\". |
| **Test Fixtures & Names** | `testExportConstNotSuggestedAtTopLevel`, `CompactPhase28IntentionsTest` | Omit completely or translate to: \"Fixed invalid completion suggestions\". |
| **File Paths & Line Numbers** | `compact/compiler/parser.ss:240-270`, `Compact.xml` | Omit completely; reference standard Compact syntax rules. |
| **Upstream Compiler Lines** | `compiler/parser.ss:460-480`, `Program-element` | \"aligned with upstream Compact grammar\". |
| **Internal AI Workflows** | `AI Instruction & Context System Restructuring`, `.ai/workflow.md` | **Strictly forbidden**: Internal AI/doc changes NEVER appear in user release notes. |
| **Test Counts & Metrics** | `519 tests passing across 55 suites` | Omit completely; test passes are an internal engineering metric. |
| **Implementation Mechanics** | `Kind.AFTER_EXPORT contextual classification to CompactCompletionContext` | \"Improved completion suggestions after `export` to show only valid declarations.\" |
| **Internal Refactorings** | `Refactored resolver cache keys to use Java 25 records` | Omit if no user-visible effect, or describe as \"Performance and memory optimizations\". |
| **Trivial / Tiny Edits** | \"Fixed typo in internal JavaDoc comment\" | Omit completely. |

#### Concrete Translation Examples

- **Example 1 (Auto-Numbering)**:
  - *Internal*: `Created CompactDeclarationNameGenerator providing universal auto-numbering (circuit1, circuit2, witness1, etc.) with gap filling and scope container isolation.`
  - *Release Note*: `Added automatic numbering for generated declarations such as circuit1, circuit2, and witness1.`
- **Example 2 (Export Completion)**:
  - *Internal*: `Expanded export completion in CompactCompletionContributor to support all upstream exportable types; added Kind.AFTER_EXPORT to CompactCompletionContext; updated CompactDeclarationInsertHandler.`
  - *Release Note*: `Added intelligent completion for all supported Compact export declarations.`
- **Example 3 (Grammar & Intentions)**:
  - *Internal*: `Disallowed top-level export const; removed expconst live template; restricted CompactToggleExportIntention; cross-verified against parser.ss:240-270 (ADR-028).`
  - *Release Note*: `Improved Compact grammar validation for export declarations and fixed incorrect completion suggestions for invalid top-level declarations.`
- **Example 4 (Ledger Scaffolding)**:
  - *Internal*: `Registered CompactLedgerInsertHandler with lookahead guard; added ledger live template expanding to export ledger $NAME$: $TYPE$; with auto-numbering.`
  - *Release Note*: `Added context-aware ledger declaration scaffolding.`

### 8.4 Internal-Only Changes Policy (Zero Leaks to Public Changelog)
Changes that are purely internal to repository maintenance, engineering workflows, or developer tooling must **NEVER appear in the user-facing release changelog**. This includes:
- AI instruction restructuring and prompt optimizations (`AGENTS.md`, `.ai/*`).
- Architectural Decision Records (`.ai/decisions/`).
- Bug knowledge base records (`.ai/bugs/`).
- Internal code refactoring with identical external behavior.
- Internal test suite additions, mocks, or fixture improvements.
- Developer tooling, Gradle build script optimizations, or CI pipeline changes.
- Repository organization and file renaming that does not affect plugin distribution.

These changes remain fully documented in the repository's git history, `.ai/` knowledge base, and commit messages.

### 8.5 Group Related Changes
At release time, **combine multiple related development entries into one coherent user-facing statement**.

Do NOT publish five technical entries for a single feature:
- *Bad (Fragmented)*:
  - Added `CompactCompletionContributor` export completion.
  - Added `AFTER_EXPORT` context.
  - Added `CompactDeclarationInsertHandler`.
  - Added `expstr`, `expen`, `expt` live templates.
  - Added unit test in `CompactCompletionTest`.
- *Good (Consolidated)*:
  - `Added intelligent completion for all supported Compact export declarations with interactive live template scaffolding.`

Prefer a small number of meaningful, high-value entries over a laundry list of technical steps.

### 8.6 Standard Keep-a-Changelog Categories & Structure
Releases must strictly use the standard Keep-a-Changelog categories where applicable:
- `### Added` — for new user-visible features, templates, or completions.
- `### Changed` — for changes in existing functionality, improved completions, or updated templates.
- `### Deprecated` — for soon-to-be removed features.
- `### Removed` — for now removed features.
- `### Fixed` — for any user-visible bug fixes or false-positive/false-negative inspection corrections.
- `### Security` — in case of vulnerabilities.

> [!WARNING]
> - Do NOT invent custom internal categories (such as `### Tested`, `### Refactored`, or `### Internal`).
> - Do NOT leave empty category headers in a release note.

#### Standard Release Structure
```markdown
<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Midnight-plugin Changelog

## [Unreleased]

## [1.2.5] - 2026-09-15

### Added
- Added intelligent autocompletion for all supported Compact export declarations (`circuit`, `ledger`, `struct`, `enum`, `type`, `module`, `contract`, `witness`), modifiers, and export selection blocks.
- Added automatic numbering for newly generated declarations with smart gap-filling.
- Added context-aware ledger declaration scaffolding.

### Changed
- Improved declaration live templates to dynamically calculate context-aware default names.
- Improved Compact grammar validation for export declarations.

### Fixed
- Fixed invalid top-level `export const` suggestions in code completion and intention actions.

## [1.2.4] - 2026-09-09
...
```
- Keep the newest release first.
- Preserve a fresh `## [Unreleased]` section at the top for future work.
- Use ISO dates (`YYYY-MM-DD`).

### 8.7 Mandatory Release-Time Changelog Review Gate
Before finalizing release notes or creating a release tag, the AI MUST review every entry against these **6 Review Questions**:
1. **Would an IntelliJ plugin user care about this?** *(If NO, remove it).*
2. **Does this explain the user-visible effect?** *(If NO, rewrite to explain what changed for the user).*
3. **Is this understandable without seeing the source code?** *(If NO, strip code symbols and rephrase).*
4. **Can multiple entries be combined?** *(If YES, consolidate into a single cohesive bullet point).*
5. **Does this contain unnecessary internal implementation detail?** *(If YES, eliminate classes, methods, paths, and ADR references).*
6. **Does this belong in an ADR, bug record, commit, or `.ai/` instead?** *(If YES, move it to the proper internal artifact).*

### 8.8 Final Release Changelog Checklist
Before pushing or creating a release tag, verify all 10 items:

```text\nRELEASE CHANGELOG CHECKLIST:
[ ] 1. Unreleased reviewed? (Every entry evaluated against user perspective)
[ ] 2. User-facing wording? (Clear, concise answers to \"What changed for me?\")
[ ] 3. Internal implementation details removed? (No classes, methods, mechanics, or AST details)
[ ] 4. ADR numbers removed? (No \"ADR-XXX\" references)
[ ] 5. Source paths, classes, and test names removed? (No file paths, compiler lines, or test methods)
[ ] 6. Related changes consolidated? (Cohesive single entries instead of fragmented technical steps)
[ ] 7. Only notable changes retained? (Trivial edits and internal noise excluded)
[ ] 8. Correct categories used? (Only standard Keep-a-Changelog categories; no empty sections)
[ ] 9. Release version and ISO date added? (e.g. ## [1.2.5] - YYYY-MM-DD)
[ ] 10. Fresh Unreleased section prepared? (Ready for future development work)
```

*(If the changelog still reads like an engineering diary, the release is NOT ready).*

### 8.9 Release Metadata & Verification Steps
Once the changelog passes the checklist:
1. **Version Bump**: Update `version` in [`gradle.properties`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/gradle.properties) (e.g., `1.2.5`).
2. **Sync Plugin XML**: Update the static fallback `<change-notes>` and `<version>` in [`src/main/resources/META-INF/plugin.xml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/resources/META-INF/plugin.xml) with the user-facing release notes.
3. **Update Machine State**: Update `project-state.yaml` and `current-state.md` with the new version and release status.
4. **Build & Test Verification**:
   - Run `./gradlew test` (must pass 100% with zero failures and zero warnings).
   - Run `./gradlew buildPlugin` to verify distribution packaging.
5. **Commit & Tag**:
   - Commit all release changes cleanly to `master`.
   - Create and push the Git release tag **only when explicitly commanded**.
