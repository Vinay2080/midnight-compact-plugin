# Custom AI Prompt: Bug Investigation, Resolution & Knowledge Base Protocol

This document defines the standardized prompt and execution playbook for investigating, fixing, verifying, and documenting bugs in `midnight-compact-plugin`.

---

## 1. Copy-Pasteable Master Prompt

When instructing an AI agent to diagnose and fix a bug, use the following prompt:

```text
You are tasked with diagnosing, fixing, verifying, and documenting a bug in midnight-compact-plugin following the official Bug Resolution Protocol. Execute these steps sequentially with zero exceptions:

1. WORKTREE ISOLATION & PRE-DEBUG SEARCH:
   - Check `git status` on master; do not discard or overwrite existing user work.
   - Create and switch to an isolated Git worktree:
     git worktree add -b ai/fix-<defect-slug> ../midnight-plugin-wt-fix-<defect-slug> master
   - Mandatory Pre-Debug Search: Search `.ai/bugs/` by subsystem, class, error message, or symptom to see if a related defect was previously investigated. Never guess when documented past solutions exist.

2. REPRODUCE WITH AUTOMATED TEST:
   - Before touching production code, write a minimal, targeted failing test reproducing the defect in `src/test/java/dev/verloren/midnight/...`.
   - Ensure the test validates the exact failure scenario (AST tree, error recovery, type resolution, completion context, or threading cancellation).
   - Run the test to confirm it reproduces the failure reliably.

3. TRACE ROOT CAUSE AGAINST COMPILER GROUND TRUTH:
   - Never apply superficial heuristics or hardcoded hacks.
   - Cite upstream compiler source (`compact/compiler/parser.ss`, `lexer.ss`, `langs.ss`) or reference plugin implementations (`intellij-rust`, `intellij-scala`, `intellij-elixir`, `Rplugin`).
   - Identify the exact mechanism causing the defect (e.g. incorrect PSI parent traversal, missing token advancement, namespace collision, ReadAction violation, or premature cache eviction).

4. IMPLEMENT FIX USING MODERN JAVA 25:
   - Write clean, modern Java 25:
     * Records for immutable data carriers and cache keys.
     * Sequenced collections: `getFirst()`, `getLast()`, `reversed()` (forbidden: `get(0)`).
     * Pattern matching for `switch` and `instanceof`, with record deconstruction.
     * Unnamed patterns and variables `_` for unused exceptions, patterns, or lambda arguments.
     * Immutable collections: `List.of()`, `Set.of()`, `Map.of()`.
     * No raw casts, no dead code, no unchecked suppressions.
   - Guard against `null` and `PsiErrorElement` nodes.
   - Obey threading invariants: PSI reads inside ReadAction; mutations on EDT inside WriteCommandAction; never block EDT.

5. CONTINUOUS POST-EDIT INSPECTION LOOP:
   - Immediately after EVERY file edit, run IntelliJ inspections via MCP:
     * `get_file_problems --filePath <absolute_path>`
     * `lint_files --files [\"<absolute_path>\"]`
   - Strict Zero-Tolerance: 0 errors (`ERROR`), 0 warnings (`WARNING`), 0 weak warnings (`WEAK WARNING`), 0 grammar/spelling mistakes, and all language-level change suggestions applied.
   - Fix all detected issues immediately before moving to the next edit.

6. TEST VERIFICATION & ZERO REGRESSIONS:
   - Run `./gradlew test` inside the worktree.
   - Confirm that the reproducing test passes AND the full test suite passes 100% with zero failures and zero compiler warnings.

7. PERSISTENT BUG KNOWLEDGE BASE RECORD:
   - Create a permanent bug report in `.ai/bugs/YYYY-MM-DD-<feature>-<title>.md` following the standard schema:
     * Subsystem, Symptoms, Trigger Conditions
     * Root Cause (citing code lines & compiler ground truth)
     * Investigation & Failed Approaches
     * Solution & Verification (tests added)
     * Prevention & Key Lessons Learned
   - Register the new record in `.ai/bugs/README.md` index table.

8. CLEAN USER CHANGELOG UPDATE:
   - Add a concise, minimal user-facing entry under `## [Unreleased]` -> `### Fixed` in `CHANGELOG.md`.
   - Strictly answer "What changed for me?".
   - NEVER include Java class names, method signatures, test counts, ADR IDs, or internal AI restructuring notes.

9. COMMIT, MERGE TO MASTER & WORKTREE CLEANUP:
   - Commit all changes on the task branch: `git commit -m "fix(<subsystem>): <concise descriptive message>"`
   - Switch to master checkout, merge branch: `git merge ai/fix-<defect-slug>`
   - Run `./gradlew test` on master to confirm post-merge integrity.
   - Remove worktree: `git worktree remove ../midnight-plugin-wt-fix-<defect-slug>`
   - Delete task branch: `git branch -d ai/fix-<defect-slug>`
   - Verify master is clean and report status in the Final Artifact Completion Table.
```

---

## 2. Detailed Bug Resolution Workflow

```text
BUG REPORT / DEFECT DETECTED
  │
  ▼
[1. Worktree Creation] ─────────── Create isolated branch ai/fix-<slug> & worktree
  │
  ▼
[2. Search Prior Bug KB] ──────── Search .ai/bugs/ for similar symptoms or previous fixes
  │
  ▼
[3. Minimal Reproducing Test] ──── Write failing automated test in src/test/
  │
  ▼
[4. Ground Truth Investigation] ── Inspect compact/compiler/ and reference plugins
  │
  ▼
[5. Implement Fix (Java 25)] ───── Modern Java 25 conforming to threading & PSI models
  │
  ▼
[6. Continuous Inspection Loop] ── get_file_problems & lint_files after EVERY edit (0 issues)
  │
  ▼
[7. Full Test Suite Validation] ── ./gradlew test (100% pass, 0 failures, 0 warnings)
  │
  ▼
[8. Create Bug Record] ─────────── Write .ai/bugs/YYYY-MM-DD-...md & register in index
  │
  ▼
[9. Clean User Changelog] ──────── Add minimal human-centric bullet to CHANGELOG.md
  │
  ▼
[10. Merge to Master & Cleanup] ── Commit, merge, verify on master, remove worktree
  │
  ▼
DEFECT RESOLVED & VERIFIED
```

---

## 3. Pre-Debug Knowledge Base Search

Before writing any fix or theorizing about a bug, agents **MUST** search `.ai/bugs/`:

```bash
# Example queries to search previous bug records
# Search by subsystem or feature
find_by_name Pattern="*completion*" SearchDirectory="<project>/.ai/bugs"
# Search by error string or symptom
grep_search Query="SideEffectGuard" SearchPath="<project>/.ai/bugs"
grep_search Query="IndexOutOfBoundsException" SearchPath="<project>/.ai/bugs"
```

### Why Pre-Debugging Search is Mandatory
- Many bugs are recurring variants of platform edge cases (e.g. `SideEffectGuard: INVOKE_LATER` during intention preview, WSL virtual file path translation, or lexer lookahead starvation).
- Reviewing past records provides verified root causes, avoiding wasted time re-discovering known platform subtleties.

---

## 4. Bug Record Standard Schema

Every resolved bug that alters production behavior must be recorded in `.ai/bugs/YYYY-MM-DD-<feature>-<title>.md` matching this exact schema:

```markdown
# Bug Record: <Title>

- **Date**: YYYY-MM-DD
- **Subsystem**: [Lexer / Parser / PSI / Resolver / Semantic / Completion / Inspections / Annotator / Run / LiveTemplates]
- **Affected Files**:
  - `src/main/java/.../AffectedClass.java`
- **Related ADRs**: ADR-XXX (or None)
- **Severity**: [Critical / Major / Moderate / Minor]

---

## 1. Symptoms & Failure Behavior
- Concise description of the observed defect or error message.
- Step-by-step reproduction sequence in IntelliJ IDEA.

## 2. Root Cause Analysis
- Detailed technical explanation of why the bug occurred.
- Citation of code lines, AST structures, threading constraints, or compiler differences (`compact/compiler/`).

## 3. Investigation & Evaluated Approaches
- What was investigated, what theories were tested, and what failed or was discarded (and why).

## 4. Solution & Implementation
- Exact architectural and code solution applied using modern Java 25.
- Key invariants preserved (threading, namespace separation, null-safety).

## 5. Verification & Tests Added
- Tests created or updated in `src/test/` to reproduce and prevent regression.
- Confirmation of `./gradlew test` execution and passing count.

## 6. Prevention & Key Lessons Learned
- Concrete guidelines for future AI agents and developers to prevent similar defects from occurring.
```

---

## 5. User Changelog Translation Rules for Bug Fixes

When updating [`CHANGELOG.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/CHANGELOG.md) under `## [Unreleased]` -> `### Fixed`:

| Forbidden Internal Detail                                                   | Required User-Facing Phrasing                                                                        |
|:----------------------------------------------------------------------------|:-----------------------------------------------------------------------------------------------------|
| `Fixed aggressive isExportPreceding check in CompactCompletionContributor.` | `Fixed issue where data type completion was incorrectly suppressed in exported declaration headers.` |
| `Fixed SideEffectGuard: INVOKE_LATER in CompactSwitchCompilerQuickFix.`     | `Fixed editor crash when previewing compiler switch quick-fixes and intention actions.`              |
| `Fixed WSL /mnt/ path translation in CompactExternalAnnotator.`             | `Fixed external compiler diagnostic range mapping when running under WSL on Windows.`                |
| `Fixed missing null check on getFirstChild() in CompactResolveUtil.`        | `Fixed intermittent IDE freeze when resolving symbols in incomplete contract files.`                 |

---

## 6. Final Bug Completion Checklist

```text
BUG COMPLETION CHECKLIST:
[ ] 1. Dedicated Git worktree and branch created?
[ ] 2. Prior bug records searched in .ai/bugs/ before debugging?
[ ] 3. Failing automated test created in src/test/ reproducing the defect?
[ ] 4. Root cause verified against compiler ground truth or reference plugins?
[ ] 5. Modern Java 25 implementation with strict threading and PSI invariants?
[ ] 6. get_file_problems and lint_files report 0 errors, 0 warnings, 0 weak warnings, 0 grammar errors?
[ ] 7. Full test suite passes 100% (./gradlew test)?
[ ] 8. Bug record written to .ai/bugs/YYYY-MM-DD-...md matching standard schema?
[ ] 9. .ai/bugs/README.md index table updated with link to new record?
[ ] 10. CHANGELOG.md updated with clean, minimal user note (zero technical noise)?
[ ] 11. Worktree branch merged to master, master verified, and worktree cleaned up?
[ ] 12. Final Artifact Completion Table included in response?
```
