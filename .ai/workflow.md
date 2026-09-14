# Task Lifecycle, Workflows, & Completion Gate

This document defines the strict, standard execution lifecycle for any software development, refactoring, bug-fixing, code review, and release task within `midnight-compact-plugin`.

---

## 1. Task Lifecycle Overview

Every task must progress sequentially through these explicit phases:

```text
START
  │
  ▼
[1. Context Loading] ────────── Load mandatory rules (AGENTS.md) + task-specific files
  │
  ▼
[2. Inspect Implementation] ─── Read existing code, tests, compiler refs, & reference plugins
  │
  ▼
[3. Plan & Align] ───────────── Check invariants, consult ADRs, formulate minimal plan
  │
  ▼
[4. Implement] ──────────────── Write modern Java 25 adhering to threading & PSI models
  │
  ▼
[5. Test & Validate] ────────── Multi-tier testing (Tiers 1–5), run ./gradlew test
  │
  ▼
[6. Code Review Self-Check] ─── Verify against Section 12 anti-bikeshedding & invariant bar
  │
  ▼
[7. Documentation Impact] ───── Apply Intelligent Documentation Decision Rules
  │
  ▼
[8. Update Documentation] ───── Make targeted updates to applicable doc/state files
  │
  ▼
[9. Verify Documentation] ───── Re-read modified files; confirm content and integrity
  │
  ▼
[10. Final Verification] ────── Re-run diff check & test suites (zero regressions)
  │
  ▼
[11. Mandatory Final Gate] ──── Execute 9-step gate and output Artifact Status Table
  │
  ▼
DONE
```

---

## 2. Phase 1: Context Loading & Task Routing

Before proposing or editing anything, the agent **MUST** load the exact set of context files required for the task type:

| Task Type | Mandatory Reading Before Action |
|:---|:---|
| **Feature Planning** | 1. [`AGENTS.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/AGENTS.md)<br>2. [`.ai/project-state.yaml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/project-state.yaml)<br>3. [`.ai/context/current-state.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/current-state.md)<br>4. [`.ai/context/architecture.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/architecture.md)<br>5. Relevant ADRs in [`.ai/decisions/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/)<br>6. Targeted sections in [`.ai/context/compact-semantics.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/compact-semantics.md) |
| **Feature Implementation** | 1. [`AGENTS.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/AGENTS.md) (Modern Java & Threading rules)<br>2. Targeted production files & tests via MCP `search_symbol` / `get_symbol_info`<br>3. [`.ai/context/intellij-patterns.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/intellij-patterns.md)<br>4. Relevant local reference plugin (`intellij-rust`, `intellij-elixir`, `intellij-scala`, `Rplugin`) |
| **Bug Fixing** | 1. Targeted failing test or fixture in `src/test/`<br>2. Targeted production implementation in `src/main/`<br>3. Official compiler ground truth (`compact/compiler/`) or [`.ai/context/compact-semantics.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/compact-semantics.md)<br>4. [`.ai/context/intellij-patterns.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/intellij-patterns.md) for platform nuances |
| **Architecture / Subsystem Change** | 1. [`AGENTS.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/AGENTS.md) (Invariants & Threading)<br>2. [`.ai/context/architecture.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/architecture.md)<br>3. Existing ADRs in [`.ai/decisions/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/) & [`.ai/decisions/README.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/README.md)<br>4. Upstream compiler sources (`compact/compiler/`) & reference plugins |
| **Code Review / Quality Audit** | 1. [`AGENTS.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/AGENTS.md) Section on Code Review (Anti-Bikeshedding Bar)<br>2. [`AGENTS.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/AGENTS.md) Section on Threading & Concurrency<br>3. Target files to review |
| **Release / Distribution Work** | 1. [`.ai/workflow.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/workflow.md) Section 7 (Release Protocol)<br>2. [`gradle.properties`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/gradle.properties)<br>3. [`CHANGELOG.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/CHANGELOG.md)<br>4. [`src/main/resources/META-INF/plugin.xml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/resources/META-INF/plugin.xml) |
| **Session Handoff & Continuation** | 1. [`.ai/handoff.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/handoff.md)<br>2. [`.ai/project-state.yaml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/project-state.yaml)<br>3. [`.ai/context/current-state.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/current-state.md) |

---

## 3. Phases 2–6: Execution & Verification

### Phase 2: Inspect Existing Implementation
- Never write code based on assumptions or generic knowledge.
- Read targeted production classes and existing test fixtures before proposing edits.
- If implementing language constructs or editor behaviors, inspect the corresponding ground truth in `compact/compiler/` and the appropriate reference plugin (`intellij-rust`, `intellij-scala`, `intellij-elixir`, `Rplugin`).

### Phase 3: Plan & Architectural Alignment
- Verify that the plan obeys all Critical Invariants in `AGENTS.md` (no replacing working architecture, reuse `CompactResolveUtil` / `CompactElementFactory`, strict namespace separation, incomplete PSI tolerance).
- Check whether an ADR already exists for this subsystem in `.ai/decisions/`.
- Ensure the plan avoids brittle heuristics and hardcoded patterns.

### Phase 4: Implementation
- Follow Java 25 standards: Java records, switch expressions with arrow syntax, unnamed variables (`_`) for unconsumed patterns/exceptions, sequenced collections, and guard clauses.
- Strictly adhere to Threading Rules: PSI reads inside `ReadAction`, PSI mutations on EDT inside `WriteCommandAction`, long operations on background threads with cancellation support.
- Guard against null and `PsiErrorElement` nodes. Never freeze the UI thread or cause `StackOverflowError`.

### Phase 5: Test & Validate
- Apply the Multi-Tier Testing Standard:
  - **Tier 1**: Golden AST tree conformance tests.
  - **Tier 2**: Partial parsing and error recovery (deliberately broken syntax must produce `PsiErrorElement` without freezing or crashing).
  - **Tier 3**: Operator precedence and associativity.
  - **Tier 4**: Stress and recursion resilience.
  - **Tier 5**: Non-blocking concurrency and cancellation.
- Run `./gradlew test` and confirm 100% passing tests with zero failures and zero compiler warnings.
- Check file health with MCP `get_file_problems` on modified Java files.

### Phase 6: Code Review Self-Check
- Evaluate changes against the anti-bikeshedding bar in `AGENTS.md`:
  - No cosmetic rewrites (no converting working loops to streams or reformatting untouched code).
  - No speculative abstractions (no premature interfaces, builders, or generic factories).
  - No subjective renames of identifiers that already follow project conventions.
  - Only introduce changes justified by threading safety, critical invariants, language semantics, or tangible bug fixes.

---

## 4. Phase 7: Intelligent Documentation Decision Rules

Do NOT blindly update every file for every change, and NEVER update `AGENTS.md` with transient project state. For every task, evaluate each documentation artifact against these exact criteria:

| Artifact | When to Update | When NOT to Update |
|:---|:---|:---|
| **[`CHANGELOG.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/CHANGELOG.md)** | User-visible features, inspections, templates, quick-fixes, bug fixes, behavior changes, or public toolchain updates under `## [Unreleased]`. | Internal refactorings with identical external behavior, typo fixes in private comments, or internal CI adjustments. |
| **ADR ([`.ai/decisions/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/))** | Architectural decisions, new subsystem designs, formalized language rules, significant AST/PSI structural changes, or non-trivial completion/refactoring algorithms. Register in [`.ai/decisions/README.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/README.md). | Routine bug fixes adhering to an existing ADR, minor test additions, or documentation improvements. |
| **[`.ai/context/current-state.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/current-state.md)** | Implementation status changes, new phase completion, new subsystem capabilities, known limitations, or baseline test metrics updates. | Code changes that do not alter the overall capability snapshot or test baseline. |
| **[`.ai/project-state.yaml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/project-state.yaml)** | Machine-readable project phase status changes (e.g. `planned` -> `complete`), passing test counts, new test suite registrations, or new ADR registrations. | Tasks that do not change test suite metrics, phase completion status, or architecture registry. |
| **[`.ai/handoff.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/handoff.md)** | Any session that produces meaningful implementation progress, architectural discoveries, unresolved blockers, or next-step priorities for the next session. | Trivial single-command query tasks where no work state changed. |
| **[`AGENTS.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/AGENTS.md)** | ONLY when permanent agent instructions, architectural invariants, tool priorities, coding conventions, or forbidden patterns themselves change. | NEVER update `AGENTS.md` for feature completions, test count bumps, or phase progression. |
| **[`plugin.xml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/resources/META-INF/plugin.xml)** | When new extensions (inspections, actions, intentions, file templates) are added, or when updating the user-facing `<description>` or release `<change-notes>`. | Internal code changes that don't add or alter IntelliJ extension points. |

---

## 5. Phase 8 & 9: Update & Mandatory Re-Read Verification

### Phase 8: Apply Updates
Apply the necessary modifications to the determined documentation and state files using the appropriate client tools.

### Phase 9: Verify Documentation Edits (Anti-Assumption Rule)
> [!CAUTION]
> **NEVER ASSUME A SUCCESSFUL EDIT TOOL CALL MEANS THE FILE IS CORRECT.**
> Tool edits can partially fail, truncate content, introduce malformed YAML/Markdown, or leave stale numbers behind.

Before moving to the final gate, the agent **MUST re-read every modified documentation or state file** using `client_view_file` or `view_file` and explicitly confirm:
1. The modified lines are present and syntactically correct.
2. No existing sections were accidentally dropped, duplicated, or truncated.
3. Test counts, version numbers, and file references are mutually consistent across all updated files.

---

## 6. Phase 10 & 11: Mandatory Final Task Gate

Before claiming that any task is complete, the agent **MUST** execute this 9-step gate in order:

```text
FINAL TASK GATE CHECKLIST:
[ ] 1. Inspect implementation diff (verify all changes match requirements).
[ ] 2. Run relevant tests (./gradlew test passes with 0 failures).
[ ] 3. Check for IDE/compiler problems (MCP get_file_problems reports 0 errors/warnings).
[ ] 4. Determine which documentation/state files are affected (via Decision Rules).
[ ] 5. Update every applicable documentation/state file.
[ ] 6. Re-read every modified documentation/state file to verify edits landed correctly.
[ ] 7. Inspect final diff again (confirm no stray files or inadvertent edits).
[ ] 8. Run final verification/tests (confirm build succeeds with zero warnings).
[ ] 9. Explicitly report the status of each artifact in the Final Artifact Completion Table.
```

### Mandatory Final Artifact Completion Table

Every task completion report **MUST** include this table with the status of each artifact:

| Artifact | Status | Details / Justification |
|:---|:---|:---|
| **`CHANGELOG.md`** | `Updated` / `Not applicable` | [Specific section updated or reason why not applicable] |
| **ADR (`.ai/decisions/`)** | `Created` / `Updated` / `Not applicable` | [ADR-XXX or reason why not applicable] |
| **`current-state.md`** | `Updated` / `Not applicable` | [Status summary or reason why not applicable] |
| **`project-state.yaml`** | `Updated` / `Not applicable` | [Metrics updated or reason why not applicable] |
| **`handoff.md`** | `Updated` / `Not applicable` | [Session handoff summary or reason why not applicable] |
| **`AGENTS.md`** | `Updated` / `Not applicable` | [Rule change or reason why not applicable] |
| **Other Context / Config** | `Updated` / `Not applicable` | [Specify file or reason why not applicable] |

---

## 7. Release & Publication Protocol

When cutting a new release or publishing to the JetBrains Marketplace:

1. **Bump Version**: Update `version` in [`gradle.properties`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/gradle.properties) (e.g. `1.2.5`).
2. **Version the Changelog**: In [`CHANGELOG.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/CHANGELOG.md), promote the items under `## [Unreleased]` into a new version header:
   ```markdown
   ## [1.2.5] - YYYY-MM-DD
   ### Added
   ...
   ```
3. **Sync Plugin XML**: Update the static fallback `<change-notes>` and `<version>` references in [`src/main/resources/META-INF/plugin.xml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/resources/META-INF/plugin.xml) to match `CHANGELOG.md`.
4. **Update Project State**: Update `project-state.yaml` and `current-state.md` with the new release version.
5. **Build & Verify Distribution**:
   - Run `./gradlew buildPlugin` to verify compilation, test passes, and plugin zip generation in `build/distributions/`.
