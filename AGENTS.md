# AI & Developer Instructions — Midnight Language Plugin

> [!CAUTION]
> ### 🛑 MANDATORY STEP 0: PRE-FLIGHT GUARD FOR ALL AGENTS
> **NEVER EDIT OR CREATE FILES DIRECTLY ON `master`.**
>
> Before executing ANY tool that modifies repository files:
> 1. **VERIFY ISOLATION**:
>    - Check `git status` on `master`. Never overwrite, stash destructively, or discard existing user work.
>    - Create and enter a dedicated worktree or dedicated branch (`ai/<task-slug>`):
>      - `git checkout -b ai/<task-slug>` (or use `.worktrees/<task-slug>`).
> 2. **CONTINUOUS VERIFICATION & INVARIANTS**:
>    - Never weaken tests or commit without explicit permission.
>    - Run `./scripts/verify-patch.ps1 -Quick` or `./gradlew test` to verify changes.

---

## 1. Project Identity & Architecture

- **Project**: Midnight Compact Language Plugin for IntelliJ IDEA (`dev.verloren.midnight`).
- **Target Language**: Compact smart contract language for the Midnight blockchain network.
- **Runtime & Toolchain**: Java 25 (`JavaLanguageVersion.of(25)`), Gradle 8.12+, IntelliJ Platform 2026.2.0.1.
- **Reference Codebases**: Consult local production plugins (`intellij-rust`, `intellij-elixir`, `intellij-scala`, `Rplugin`) and `compact/` compiler sources in the workspace.

```text
Compact Source Text (.compact)
  ↓
[Lexer] CompactLexer (extends LexerBase) + CompactTokenTypes
  ↓
[Parser] CompactParser (implements PsiParser) + CompactElementTypes
  ↓
[PSI] CompactPsiElement / CompactFile / Typed AST Wrappers (dev.verloren.midnight.psi.impl.*)
  ↓
[Resolve & Scope] CompactResolveUtil (Innermost shadowing, split VALUE/TYPE namespaces, cross-file includes)
  ↓
[Semantic Layer] CompactTypeInferenceUtil + Semantic Inspections & Quick-Fixes
  ↓
[IDE Features] Completion, Smart Enter, In-Editor Intentions, Rename, Find Usages, Formatter,
               Structure View, Quick Docs, Run Configurations, External Linter Annotator, Tool Windows
```

---

## 2. Non-Negotiable Core Invariants

1. **Handwritten Parser & Lexer**:
   - `CompactParser` and `CompactLexer` are handwritten. Do NOT replace them with Grammar-Kit or JFlex generated code.
   - **Anti-Freeze Guard**: Parser loops must advance tokens (`builder.advanceLexer()`) on every iteration.
2. **Tolerance for Incomplete Code**:
   - All PSI operations, annotators, formatters, and doc providers must guard against `null` and `PsiErrorElement`.
3. **Strict Namespace Separation**:
   - Strictly separate `CompactResolveUtil.Namespace.VALUE` and `CompactResolveUtil.Namespace.TYPE`.
   - Resolving a type must never return a variable/constant; resolving a value expression must never return a type.
4. **Threading & Concurrency Rules**:
   - **PSI Read**: Background thread or EDT with `ReadAction` (`ReadAction.compute()` / `ReadAction.run()`).
   - **PSI Mutation**: Strictly on Event Dispatch Thread (EDT) wrapped in `WriteCommandAction`.
   - **Zero EDT CLI Operations**: NEVER call `process.waitFor()` or run blocking CLI tools (`compact`, `wsl`) on EDT.
5. **Memory Leak Prevention**:
   - NEVER store `PsiElement`, `PsiFile`, `Document`, or `Project` in static fields, long-lived caches, or non-disposable listeners.
   - Use `CachedValuesManager` keyed to `PsiModificationTracker.MODIFICATION_COUNT`.
6. **Modern Java 25 Standard**:
   - Use records for data carriers, sequenced collections (`getFirst()`/`getLast()`), pattern matching switch, and unnamed variables (`_`).

---

## 3. Path-Scoped Subsystem Rules

Detailed rules and code patterns are modularized into `.agents/rules/`:

| Subsystem | Rules File | Scope & Key Constraints |
|:---|:---|:---|
| **Architecture & Modularity** | [`.agents/rules/architecture.rules.md`](file:///.agents/rules/architecture.rules.md) | Layer hierarchy (downward only), $\le 400$ lines/class, $\le 40$ lines/method, anti-hardcoding. |
| **PSI & Parser** | [`.agents/rules/psi-parser.rules.md`](file:///.agents/rules/psi-parser.rules.md) | Anti-freeze token advancement guarantee, incomplete AST tolerance, dual namespaces. |
| **Threading & Concurrency** | [`.agents/rules/threading.rules.md`](file:///.agents/rules/threading.rules.md) | ReadAction queries, EDT WriteCommandAction mutations, no blocking CLI on EDT, static PSI prohibition. |
| **Modern Java 25** | [`.agents/rules/modern-java25.rules.md`](file:///.agents/rules/modern-java25.rules.md) | Records, sequenced collections (`getFirst`/`getLast`), pattern matching, unnamed variables (`_`), text blocks. |
| **Inspections & Annotators** | [`.agents/rules/inspections-annotators.rules.md`](file:///.agents/rules/inspections-annotators.rules.md) | `SideEffectGuard` in preview sessions, 3-phase `ExternalAnnotator` lifecycle. |
| **Toolchain & WSL** | [`.agents/rules/toolchain-wsl.rules.md`](file:///.agents/rules/toolchain-wsl.rules.md) | Filter Windows `System32/compact.exe`, WSL path translations, process timeouts. |

---

## 4. Context Navigation & Knowledge Map

All contextual assets and documentation live under `.ai/` and `docs/`:

| Resource | Path | Description |
|:---|:---|:---|
| **Improvement Roadmap** | [`docs/engineering/ENGINEERING_IMPROVEMENT_ROADMAP.md`](file:///docs/engineering/ENGINEERING_IMPROVEMENT_ROADMAP.md) | Phased engineering improvement and quality enforcement plan. |
| **Workflow Lifecycle** | [`.ai/workflow.md`](file:///.ai/workflow.md) | Development lifecycle, documentation decision rules, and completion gates. |
| **Push & Release** | [`.ai/prompts/push-and-release.md`](file:///.ai/prompts/push-and-release.md) | Prompts and checklist for changelog cleanup, tagging, and pushing. |
| **Machine State** | [`.ai/project-state.yaml`](file:///.ai/project-state.yaml) | Machine-readable feature phases, test counts, and active modules. |
| **Architecture Guide** | [`.ai/context/architecture.md`](file:///.ai/context/architecture.md) | Detailed subsystem design, threading model, and extension points. |
| **Compact Semantics** | [`.ai/context/compact-semantics.md`](file:///.ai/context/compact-semantics.md) | Verified Compact language syntax, typing rules, and grammar. |
| **Architectural Decisions** | [`.ai/decisions/README.md`](file:///.ai/decisions/README.md) | Index of Architectural Decision Records (`ADR-001` through `ADR-034`). |
| **Bug Knowledge Base** | [`.ai/bugs/README.md`](file:///.ai/bugs/README.md) | Searchable index of resolved debugging incidents and lessons learned. |
| **User Changelog** | [`CHANGELOG.md`](file:///CHANGELOG.md) | Curated user-facing release notes (keep clean of internal engineering details). |

---

## 5. Verification & Quality Gates

Run the verification runner before claiming task completion:

```powershell
# Fast compilation and plugin structure check
powershell -ExecutionPolicy Bypass -File .\scripts\verify-patch.ps1 -Quick

# Targeted test verification
powershell -ExecutionPolicy Bypass -File .\scripts\verify-patch.ps1 -TestPattern dev.verloren.midnight.CompactBundleTest

# Full test suite execution
./gradlew test
```

---

## 6. Protected Artifacts Guard

The following files and contracts are **PROTECTED ARTIFACTS**. AI agents must not modify or weaken them without explicit human approval:
1. **Core Architectural Invariants** in Section 2.
2. **Accepted Architectural Decision Records** in [`.ai/decisions/`](file:///.ai/decisions/).
3. **Security Boundaries & Vulnerability Policies** in [`.ai/security/`](file:///.ai/security/).
4. **Existing Unit / Integration Tests**: Never delete or ignore failing tests to artificially pass verification gates.
