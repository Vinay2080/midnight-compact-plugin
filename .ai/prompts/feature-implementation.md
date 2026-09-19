# Custom AI Prompt: Feature Planning, Implementation & State Synchronization Protocol

This document defines the standardized prompt and execution playbook for planning and implementing new language features, editor behaviors, inspections, live templates, and platform integrations in `midnight-compact-plugin`.

---

## 1. Copy-Pasteable Master Prompt

When instructing an AI agent to design and implement a feature, use the following prompt:

```text
You are tasked with implementing a new feature in midnight-compact-plugin following the official Feature Implementation Protocol. Execute these steps sequentially with zero exceptions:

1. WORKSPACE ISOLATION & SPECIFICATION CHECK:
   - Check `git status` on master; never overwrite or discard uncommitted user work.
   - Switch to a dedicated in-workspace task branch:
     git checkout -b ai/<feature-slug>
     (Note: Operate directly in the workspace or within an in-workspace .worktrees/<feature-slug> directory so IntelliJ MCP tools like get_file_problems and client file tools operate with full workspace access. Avoid external ../ worktree paths).
   - Review feature requirements against Compact compiler ground truth (`compact/compiler/parser.ss`, `standard-library.compact`) and reference plugins (`intellij-rust`, `intellij-scala`, `intellij-elixir`, `Rplugin`).

2. ARCHITECTURAL ALIGNMENT & ADR CHECK:
   - Review `AGENTS.md` and `.ai/context/architecture.md`.
   - Check existing ADRs in `.ai/decisions/` to ensure the design aligns with existing architectural conventions.
   - If the feature introduces a new subsystem, non-trivial AST redesign, or new indexing mechanism, draft an ADR using `.ai/prompts/architecture-decision.md`.

3. MODERN JAVA 25 IMPLEMENTATION:
   - Write modern, idiomatic Java 25:
     * Records (`record`) for immutable data structures, AST node pairs, resolver cache keys, and DTOs.
     * Sequenced Collections (`getFirst()`, `getLast()`, `reversed()`). Forbidden: `get(0)`.
     * Pattern matching switch expressions with arrow syntax (`->`) and record deconstruction patterns.
     * Unnamed variables and patterns (`_`) for unused arguments or exceptions.
     * Immutable collection factories (`List.of()`, `Set.of()`, `Map.of()`).
     * Multi-line text blocks (`"""..."""`) for templates, messages, and snippets.
   - Strict Threading Rules:
     * PSI reads inside ReadAction.
     * PSI mutations on EDT inside `WriteCommandAction.runWriteCommandAction(project, () -> ...)`.
     * Zero `process.waitFor()` on EDT; zero background PSI mutations.
   - AST & Parser Resilience: Guard against `null` and `PsiErrorElement` nodes. Loops must advance tokens to prevent UI thread hangs.

4. MULTI-TIER TEST IMPLEMENTATION:
   - Write comprehensive automated tests in `src/test/`:
     * Tier 1: Golden AST tree tests (`.compact` -> `.txt`).
     * Tier 2: Incomplete/broken code recovery and partial parsing.
     * Tier 3: Precedence, associativity, or completion scoping.
     * Tier 4: Cancellation and stress resilience.

5. CONTINUOUS POST-EDIT INSPECTION LOOP:
   - Immediately after EVERY file edit, run IntelliJ inspections via MCP:
     * `get_file_problems --filePath <absolute_path>`
     * `lint_files --files [\"<absolute_path>\"]`
   - Strict Zero-Tolerance: 0 errors (`ERROR`), 0 warnings (`WARNING`), 0 weak warnings (`WEAK WARNING`), 0 grammar/spelling errors, and all language-level suggestions applied.
   - Fix all detected issues immediately before moving to the next edit.

6. TEST VERIFICATION:
   - Run `./gradlew test`.
   - Confirm 100% pass rate with zero failures and zero compiler warnings.

7. STATE & DOCUMENTATION SYNCHRONIZATION:
   - Update `.ai/context/current-state.md`: update capability snapshot, completed phase status, and test metrics table.
   - Update `.ai/project-state.yaml`: update passing test counts, test suites, and phase statuses.
   - Update `CHANGELOG.md` under `## [Unreleased]` -> `### Added` / `### Changed`:
     * Write clean, minimal, human-centric user notes answering "What changed for me?".
     * NEVER include internal class names, method signatures, test counts, or ADR numbers.
   - Update `.ai/handoff.md` with active session status and next priorities.

8. ATOMIC COMMITS, MERGE TO MASTER, PUSH & CLEANUP:
   - Stage and commit changes using atomic, conventional commits on the task branch:
     * Feature & Tests:
       git add src/ && git commit -m "feat(<subsystem>): <concise descriptive message>"
     * Context & State Synchronization:
       git add .ai/ CHANGELOG.md && git commit -m "docs(context): record <feature> implementation and update test metrics"
   - Switch to master: `git checkout master`
   - Non-destructive merge: `git merge ai/<feature-slug>`
   - Run `./gradlew test` on master to confirm post-merge integrity.
   - Push verified commits to remote:
     git push origin master
   - Delete temporary task branch: `git branch -d ai/<feature-slug>`
   - Report status in the Final Artifact Completion Table.
```

---

## 2. Feature Implementation Checklist

```text
FEATURE COMPLETION CHECKLIST:
[ ] 1. Workspace status inspected and in-workspace task branch created (ai/<feature-slug>)?
[ ] 2. Compact language ground truth verified in compact/compiler/?
[ ] 3. Reference plugins inspected for IntelliJ platform idioms?
[ ] 4. Modern Java 25 implemented (records, sequenced collections, pattern matching, unnamed variables)?
[ ] 5. PSI threading model strictly respected (ReadAction, WriteCommandAction on EDT)?
[ ] 6. get_file_problems and lint_files report 0 errors, 0 warnings, 0 weak warnings, 0 grammar errors?
[ ] 7. Multi-tier tests added and ./gradlew test passes 100%?
[ ] 8. .ai/context/current-state.md and .ai/project-state.yaml updated with new status and test counts?
[ ] 9. CHANGELOG.md updated under ## [Unreleased] with clean, minimal user note?
[ ] 10. Atomic commits created on task branch (feat(...) and docs(...))?
[ ] 11. Task branch merged to master, post-merge tests verified, and pushed to remote (git push origin master)?
[ ] 12. Task branch cleaned up and Final Artifact Completion Table provided?
```
