# Custom AI Prompt: Code Inspection, Clean Changelog, Semantic Tagging & Push Protocol

This document defines the standardized custom prompt, execution protocol, and verification rules for inspecting code, cleaning the user-facing changelog, determining semantic release tags, and pushing code and tags to the remote repository.

---

## 1. Copy-Pasteable Master Prompt

When instructing an AI agent or executing a release/push session, use the following prompt:

```text
You are tasked with executing the Midnight Plugin Code Inspection, Clean Changelog, Semantic Tagging, and Push Protocol. Execute these 4 mandatory steps sequentially with zero exceptions:

1. COMPREHENSIVE PROJECT INSPECTION & FIX:
   - Run IDE inspection tools (`get_file_problems` and `lint_files` via `execute_tool`) on all modified, added, and referenced files, plus run `./gradlew test` and `./gradlew check`.
   - The project MUST NOT contain ANY kind of:
     * Unwanted warnings (`WARNING`)
     * Weak warnings (`WEAK WARNING`)
     * Grammar or spelling errors in code, comments, JavaDocs, UI messages, intentions, or documentation
     * Language-level change suggestions provided by inspection tools (e.g. upgrading to modern Java 25 features: record patterns, sequenced collections `getFirst()`/`getLast()`, pattern matching for switch/instanceof, unnamed variables `_`, text blocks, modern immutable collection factories `List.of()`, redundant type arguments `<>`, redundant casts, or unused parameters)
     * Deprecated API usages, raw types, unchecked operations, or dead code.
   - FIX all detected problems immediately.

2. CONTINUOUS INSPECTION-FIX-REINSPECT LOOP:
   - Before doing this, during edits, and after: whenever an issue is flagged or code is touched, fix it immediately.
   - Re-run inspections after every fix to confirm that no secondary warnings or regressions are introduced.
   - Loop until ZERO errors, ZERO warnings, and ZERO weak warnings remain across the entire codebase.

3. DUAL-TIER CHANGELOG MANAGEMENT:
   - Understand the strict separation of concerns:
     * Code & Engineering Updates (for AI & Developers): Exhaustive technical details, AST node names, class/method names, compiler line references, test counts, and internal AI documentation restructuring belong EXCLUSIVELY in `.ai/` (`.ai/bugs/`, `.ai/decisions/`, `.ai/context/`, `.ai/project-state.yaml`, `.ai/handoff.md`), Git commit messages, and code comments.
     * User Changelog (`CHANGELOG.md`): Written strictly for IntelliJ plugin users, answering "What changed for me?". It must be clean, minimal, human-centric, and categorized under standard Keep-a-Changelog sections (`### Added`, `### Changed`, `### Deprecated`, `### Removed`, `### Fixed`). Strip ALL internal class names, method names, AST mechanics, ADR numbers, test counts, file paths, and internal AI notes. Consolidate fragmented technical commits into cohesive user-facing statements.

4. SEMANTIC TAG SELECTION, VERSION BUMP & PUSH:
   - Based on the type of changes and features, determine the appropriate semantic version tag:
     * MAJOR (`v(X+1).0.0`): Breaking Compact grammar/compiler changes, breaking API/SDK shifts, or major architectural overhauls.
     * MINOR (`vX.(Y+1).0`): New user-facing features, new declarations, new live templates, new intentions, new inspections/quick fixes, new tool windows/UI features, or non-breaking enhancements.
     * PATCH (`vX.Y.(Z+1)`): Bug fixes, false-positive/false-negative inspection corrections, editor stability fixes, performance optimizations, or minor non-breaking enhancements.
   - Update `gradle.properties` with the new version (`version=X.Y.Z`).
   - Sync fallback `<version>` and `<change-notes>` in `src/main/resources/META-INF/plugin.xml`.
   - In `CHANGELOG.md`, convert `## [Unreleased]` into `## [X.Y.Z] - YYYY-MM-DD` and open a fresh empty `## [Unreleased]`.
   - Update machine state in `.ai/project-state.yaml` and `.ai/context/current-state.md`.
   - Run full test suite `./gradlew test -Pversion=X.Y.Z` and `./gradlew buildPlugin` to verify packaging.
   - Commit all release changes to `master` (`git commit -m "chore(release): prepare vX.Y.Z"`).
   - Create an annotated Git tag: `git tag -a vX.Y.Z -m "Release vX.Y.Z: <clean user-facing summary>"`.
   - Push code and tag to remote: `git push origin master` and `git push origin vX.Y.Z`.
```

---

## 2. Detailed Step-by-Step Execution Protocol

### Step 1: Deep Project Inspection & Language-Level Modernization

Code quality is enforced through strict, zero-tolerance static analysis.

#### 1.1 Run IntelliJ Live Problem Inspection
For every modified or referenced source file:
```bash
# Via idea execute_tool
execute_tool --command "get_file_problems --filePath <absolute_path>"
```
Check the `errors` array. Every single item with severity `ERROR`, `WARNING`, or `WEAK WARNING` must be resolved.

#### 1.2 Run Static Inspection & Linter
```bash
# Via idea execute_tool (quote JSON array properly)
execute_tool --command "lint_files --files [\"<absolute_path>\"]"
```

#### 1.3 Inspection Categories to Catch and Eliminate
| Problem Type | Description | Mandatory Action |
|:---|:---|:---|
| **Compilation Errors (`ERROR`)** | Unresolved symbols, syntax errors, type mismatches. | Fix immediately. |
| **Warnings (`WARNING`)** | Deprecated APIs, unchecked casts, raw types, unhandled exceptions, unused declarations. | Fix immediately; no suppressions unless strictly required by external platform APIs. |
| **Weak Warnings (`WEAK WARNING`)** | Suboptimal calls, redundant qualifiers, simplifyable expressions, code smells. | Fix immediately. Replace legacy calls with modern equivalents. |
| **Grammar & Spelling Errors** | Typos and grammar issues in comments, JavaDocs, user-facing error messages, quick-fix descriptions, and intention names. | Fix immediately using proper English grammar and accurate terminology. |
| **Language-Level Change Suggestions** | Suggestions to adopt modern Java 25 language features. | **Mandatory upgrade**: |
| - *Sequenced Collections* | `list.get(0)` or `list.get(list.size() - 1)` | Replace with `list.getFirst()` and `list.getLast()`. |
| - *Pattern Matching* | `if (x instanceof Foo) { Foo f = (Foo) x; ... }` | Replace with `if (x instanceof Foo f) { ... }`. |
| - *Pattern Switch* | Traditional cascading `if-else` or legacy `switch` | Replace with modern enhanced switch expressions and pattern matching. |
| - *Record Deconstruction* | Accessing components via getters manually | Use record pattern deconstruction `if (obj instanceof MyRecord(var a, var b))`. |
| - *Unnamed Variables* | Unused catch variables or lambda parameters | Use unnamed pattern `_` (Java 22+). |
| - *Text Blocks* | Concatenated strings for multi-line text or templates | Convert to Java text blocks `"""..."""`. |
| - *Redundant Types & Diamond* | Explicit generic parameters where inferred | Use diamond operator `<>` or `var`. |
| - *Immutable Collections* | `Collections.unmodifiableList(...)` or manual loops | Use `List.of()`, `Set.of()`, `Map.of()`. |

---

### Step 2: The Continuous Fix-and-Reinspect Loop

```text
┌────────────────────────────────────────────────────────┐
│               Trigger Inspection Phase                 │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
           Run get_file_problems & lint_files
                            │
                            ▼
                Issues Found in Output?
                 ├── YES ──► Fix all detected issues immediately
                 │           │
                 │           ▼
                 │           Re-inspect file (Loop until 0 issues remain)
                 │
                 └── NO (100% Clean)
                            │
                            ▼
              Run `./gradlew test` & Verify
                            │
                            ▼
             Proceed to Changelog & Tagging
```

- **Before doing changes**: Verify baseline cleanliness.
- **During changes**: Inspect immediately after EVERY file edit. Never batch inspections at the end.
- **After changes**: Re-inspect all modified files and run `./gradlew test` to ensure zero regressions.
- **Loop until**: Exactly 0 errors, 0 warnings, 0 weak warnings, 0 grammar errors, and 0 language level suggestions remain.

---

### Step 3: Dual-Tier Changelog Management

#### 3.1 The Two Audiences
1. **For AI Agents & Developers (Exhaustive Engineering Knowledge)**:
   - Full implementation mechanics, AST nodes, PSI wrappers, cache keys.
   - Upstream compiler line citations (`compact/compiler/parser.ss`).
   - Reference plugin comparisons (`intellij-rust`, `intellij-scala`, `intellij-elixir`, `Rplugin`).
   - Architectural Decision Records in [`.ai/decisions/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/).
   - Detailed root cause investigations in [`.ai/bugs/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/bugs/).
   - Git commit messages with conventional commit scopes and detailed bodies.
   - Comprehensive JavaDoc and inline code comments explaining *why* design decisions were made.

2. **For Plugin Users ([`CHANGELOG.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/CHANGELOG.md))**:
   - Clean, minimal, concise, human-centric.
   - Strictly answers: *"What changed for me?"*
   - Categorized under Keep-a-Changelog standard headers (`### Added`, `### Changed`, `### Deprecated`, `### Removed`, `### Fixed`).
   - **Zero technical noise**: No Java class names, method names, AST node types, file paths, line numbers, test counts, ADR identifiers, or AI workflow details.
   - Multiple fragmented development commits are consolidated into 1–3 high-level user-facing bullet points.

#### 3.2 Before & After Translation Examples

| Internal Developer / AI Note | Clean Minimal User Changelog Note |
|:---|:---|
| `Added CompactAngleBraceTypedHandler and CompactAngleBraceBackspaceHandler for auto-pairing <> with overtyping support.` | `Added automatic angle bracket (<>) pairing, overtyping, and paired deletion for generic types.` |
| `Added CompactTypeExpression and registered CompactTypeMacro (compactType(...)) for interactive template type completion popups.` | `Added automatic data type completion popups when tabbing through declaration live templates.` |
| `Fixed aggressive isExportPreceding heuristic in CompactCompletionContributor and CompactCompletionContext that hijacked type slots.` | `Fixed issue where data type completion was incorrectly suppressed in exported declaration headers.` |
| `Suppressed daemon code analyzer restarts and project SDK mutation during intention preview generation to avoid SideEffectGuard exception.` | `Fixed editor crash when previewing compiler switch quick-fixes and intentions.` |
| `Updated WSL /mnt/<drive>/ path translation and normalized virtual file paths in CompactExternalAnnotator.` | `Fixed external compiler diagnostic mapping when running under WSL on Windows.` |
| `AI Instruction & Context System Restructuring and workflow updates.` | *(Omit completely — internal AI / developer documentation never leaks into user changelog).* |

---

### Step 4: Semantic Tag Selection, Version Bump & Push Protocol

#### 4.1 Semantic Versioning Decision Matrix

| Change Type | Impact on User / Language | SemVer Bump | Tag Format | Examples in Midnight Plugin |
|:---|:---|:---|:---|:---|
| **Breaking / Major** | Incompatible Compact syntax changes, deprecated language feature removals, major IDE platform baseline upgrade (e.g. IntelliJ 2026.1 -> 2027.1). | **MAJOR** (`X+1.0.0`) | `v2.0.0` | Breaking Compact type system restructuring, major parser re-architecture. |
| **New Features / Minor** | New user-visible language constructs, new live templates, new intention actions, new semantic inspections & quick-fixes, new tool windows (Remix compiler, status bar widget), new run configurations. | **MINOR** (`X.Y+1.0`) | `v1.3.0` | Angle bracket auto-pairing, template type macro completion, export ledger templates, status bar monitor. |
| **Fixes & Tuning / Patch** | Bug fixes, false-positive inspection fixes, editor crash/freeze prevention, diagnostic line offset fixes, performance optimizations, non-breaking completion refinements. | **PATCH** (`X.Y.Z+1`) | `v1.2.7` | Fixing stale annotator underlines, fixing `SideEffectGuard` intention preview exceptions, fixing keyword duplicate completions. |

#### 4.2 Version Synchronization & Build Verification
1. **Bump Version in `gradle.properties`**:
   ```properties
   version=1.2.7
   ```
2. **Sync Fallback Release Metadata in `src/main/resources/META-INF/plugin.xml`**:
   Update `<version>` and `<change-notes>` with the clean, user-facing release notes.
3. **Format User `CHANGELOG.md`**:
   Convert `## [Unreleased]` into `## [X.Y.Z] - YYYY-MM-DD` and open a fresh empty `## [Unreleased]` at the top.
4. **Update Machine State**:
   Update `project-state.yaml` and `current-state.md` with the new version.
5. **Run Verification**:
   ```bash
   ./gradlew test -Pversion=X.Y.Z
   ./gradlew buildPlugin -Pversion=X.Y.Z
   ```

#### 4.3 Git Commit, Tagging, and Push Commands
```bash
# 1. Commit release changes to branch
git add gradle.properties src/main/resources/META-INF/plugin.xml CHANGELOG.md .ai/
git commit -m "chore(release): prepare v1.2.7"

# 2. Merge to master (if operating in a worktree)
git checkout master
git merge --ff-only ai/<task-slug>

# 3. Create annotated tag
git tag -a v1.2.7 -m "Release v1.2.7: <concise user-facing feature summary>"

# 4. Push code and tag to remote
git push origin master
git push origin v1.2.7
```

---

## 3. Pre-Push & Release Verification Checklist

Before pushing any commit or tag, confirm every checkbox:

```text
RELEASE & PUSH CHECKLIST:
[ ] 1. All modified/new files inspected via get_file_problems and lint_files?
[ ] 2. Zero errors (0 ERROR)?
[ ] 3. Zero warnings (0 WARNING)?
[ ] 4. Zero weak warnings (0 WEAK WARNING)?
[ ] 5. Zero grammar and spelling errors in code, comments, JavaDocs, and strings?
[ ] 6. All modern Java 25 language-level suggestions applied (sequenced collections, pattern matching, record deconstruction, unnamed variables, text blocks)?
[ ] 7. Continuous fix loop completed (inspected before, during, and after; re-verified clean)?
[ ] 8. CHANGELOG.md is clean, minimal, and written strictly for plugin users?
[ ] 9. All internal classes, methods, AST nodes, ADR numbers, and AI notes kept strictly in .ai/ and out of CHANGELOG.md?
[ ] 10. Appropriate SemVer bump chosen (Major / Minor / Patch) based on change type?
[ ] 11. gradle.properties and plugin.xml versions synchronized?
[ ] 12. ./gradlew test and ./gradlew buildPlugin passed 100%?
[ ] 13. Annotated tag created with 'v' prefix (e.g. v1.2.7)?
[ ] 14. Code and tag pushed to remote?
```
