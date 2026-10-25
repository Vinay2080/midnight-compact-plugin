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
>    - **STRICT MCP-FIRST MANDATE**: Always prioritize JetBrains IDE MCP tools (`get_file_problems`, `lint_files`, `execute_run_configuration`) FIRST for all diagnostics, code navigation, formatting, and test execution. Terminal / CLI commands (`./scripts/verify-patch.ps1 -Quick`, `./gradlew test`) are secondary verification or fallback.

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
7. **Strict JetBrains MCP Priority Over Terminal / CLI Commands**:
   - For all code intelligence, diagnostics, symbol lookup, formatting, refactoring, and test execution, AI agents **MUST strictly prioritize JetBrains MCP tools first** (`jetbrains_air_ide` / `idea` MCP server):
     * **Diagnostics & Static Problems**: Always run `get_file_problems` and `lint_files` via MCP before running terminal compilers or CLI linters.
     * **Symbol Discovery & Navigation**: Always use `search_symbol` and `get_symbol_info` via MCP rather than CLI grep, ripgrep, find, or Select-String.
     * **Code Formatting & Refactoring**: Always use `reformat_file` and `rename_refactoring` via MCP before manual or CLI transforms.
     * **Test Execution**: Always invoke `execute_run_configuration` via MCP first for running tests in the IDE environment.
   - Terminal / CLI commands (`powershell`, `verify-patch.ps1`, `./gradlew`) are secondary verification, fallback, or packaging tools when MCP is unavailable or for final multi-gate CI validation. CLI commands must NEVER take priority over JetBrains MCP tools.
8. **Architectural Javadocs & Continuous Codebase Documentation**:
   - Whenever introducing, fixing, or modifying code (bug fixes, refactoring, or new features), agents and developers must update and enrich class-level Javadocs and inline comments adhering to [`.ai/prompts/document-codebase.md`](file:///prompts/document-codebase.md).
   - Document caller subsystems, threading contracts (`@RequiresReadLock`, EDT, `WriteCommandAction`), lookahead edge cases, and step-by-step algorithms.
   - Never leave newly added or modified methods undocumented, and never leave existing comments stale or out-of-sync with implementation changes.

---

## 3. Tooling Precedence: Strict JetBrains MCP Priority Over CLI

Whenever an AI agent executes operations in this repository, it **MUST adhere to the following strict precedence**:

```text
[Operation]                 [Priority 1: JetBrains MCP (Mandatory First)]       [Priority 2: CLI / Terminal (Secondary / Fallback)]
─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
Code Diagnostics & Errors   execute_tool get_file_problems --filePath <path>    ./gradlew checkstyleMain / compileJava
Full Inspection & Style     execute_tool lint_files --files ["<path>"]          ./gradlew check / qodana
Symbol Search & Lookup      execute_tool search_symbol --q <name>               git grep / ripgrep / Select-String
API Docs & Signatures       execute_tool get_symbol_info --filePath <path>      cat / grep / manual file scanning
Code Formatting             execute_tool reformat_file --filePath <path>        ./gradlew spotlessApply / manual edits
Safe Symbol Rename          execute_tool rename_refactoring --filePath <path>   find-and-replace / sed / manual edits
Test Execution & Triage     execute_tool execute_run_configuration --name <cfg> .\scripts\verify-patch.ps1 / ./gradlew test
Multi-Gate CI / Packaging   N/A (IDE context)                                   .\scripts\verify-patch.ps1 -AllTests / buildPlugin
```

AI agents must **never default to CLI commands** when a corresponding JetBrains MCP tool exists. Always query the IDE via MCP first.

---

## 4. Path-Scoped Subsystem Rules

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

## 5. Context Navigation & Knowledge Map

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
| **Codebase Documentation Standard** | [`.ai/prompts/document-codebase.md`](file:///prompts/document-codebase.md) | Standard for architectural Javadocs, inline comments, threading contracts, and lifecycle documentation. |
| **User Changelog** | [`CHANGELOG.md`](file:///CHANGELOG.md) | Curated user-facing release notes (keep clean of internal engineering details). |

---

## 6. Verification & Quality Gates

AI agents must strictly follow the **MCP-First Verification Pipeline**:

### Primary (Priority 1): JetBrains IDE MCP Verification
Execute in-IDE diagnostics, code inspections, and test run configurations immediately after editing:
```text
# 1. Live Problem & Diagnostics Check (Zero Errors, Warnings, Weak Warnings)
execute_tool get_file_problems --filePath "<absolute-path>"

# 2. Comprehensive Code Inspection & Style Linting
execute_tool lint_files --files [\"<absolute-path>\"]

# 3. Targeted Test Execution via IDE Run Configuration
execute_tool execute_run_configuration --name "<TestRunConfigurationName>"
```

### Secondary / Final Gate (Priority 2): Multi-Gate Script & CI Runner
Run the verification runner to validate build packaging, plugin structure, and full CI regression:

```powershell
# Fast compilation and plugin structure check
powershell -ExecutionPolicy Bypass -File .\scripts\verify-patch.ps1 -Quick

# Targeted test verification
powershell -ExecutionPolicy Bypass -File .\scripts\verify-patch.ps1 -TestPattern dev.verloren.midnight.CompactBundleTest

# Full test suite execution
./gradlew test
```

---

## 7. Protected Artifacts Guard

The following files and contracts are **PROTECTED ARTIFACTS**. AI agents must not modify or weaken them without explicit human approval:
1. **Core Architectural Invariants** in Section 2 (including MCP-first priority).
2. **Accepted Architectural Decision Records** in [`.ai/decisions/`](file:///.ai/decisions/).
3. **Security Boundaries & Vulnerability Policies** in [`.ai/security/`](file:///.ai/security/).
4. **Existing Unit / Integration Tests**: Never delete or ignore failing tests to artificially pass verification gates.
