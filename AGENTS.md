# AI & Developer Instructions — Midnight Language Plugin

## 1. Project Identity
- **Project**: Midnight Compact Language Plugin for IntelliJ IDEA (`dev.verloren.midnight`).
- **Purpose**: First-class development support for the Midnight blockchain's Compact smart contract language.
- **Language**: Java 25 (Gradle toolchain `JavaLanguageVersion.of(25)`).
- **Core Status**: Lexer, Parser, PSI, References, Completion, Smart Enter, In-Editor Intentions, Refactoring, Find Usages, Type Inference, Inspections, Formatter, Smart Indentation, Structure View, Documentation Provider, Cross-File Resolution, File/Live Templates, Folding, Breadcrumbs, Run Configurations, External Annotator, Line Markers, Multi-Version Compiler Management, Status Bar Environment Monitor, Remix-Style Compiler Tool Window, Pragma Quick-Fixes, and Bundled Stdlib are implemented and verified (**472 unit tests passing across 55 test suites with 0 failures and 0 warnings**).

---

## 2. Architecture Pipeline

```text
Compact Source Text (.compact)
  ↓
[Lexer] CompactLexer (extends LexerBase) + CompactTokenTypes
  ↓
[Parser] CompactParser (implements PsiParser) + CompactElementTypes
  ↓
[PSI] CompactPsiElement / CompactFile / Typed AST Wrappers (dev.verloren.midnight.psi.impl.*)
  ↓
[Resolve & Scope] CompactResolveUtil (Innermost shadowing, split VALUE/TYPE namespaces, cross-file includes & imports)
  ↓
[Semantic Layer] CompactTypeInferenceUtil + Semantic Inspections & Quick-Fixes
  ↓
[IDE Features] Completion, Smart Enter (Ctrl+Shift+Enter), In-Editor Intentions (Alt+Enter),
               Rename, Find Usages, Formatter, Smart Indent, Structure View, Docs, Run Configurations,
               External Linter Annotator, Line Markers, Status Bar Widget, Multi-Version Toolchain & Remix Tool Window
```

---

## 3. Critical Invariants

1. **Do not replace working architecture**: The handwritten lexer, parser, PSI wrappers, resolver, structure view, and docs provider are mature and verified. Do not replace them with generated parsers or external tools without an explicit directive.
2. **Reuse existing PSI and resolve infrastructure**: Always use `CompactResolveUtil` for symbol lookups and `CompactElementFactory` for PSI node generation.
3. **Strict namespace separation**: Maintain distinct `CompactResolveUtil.Namespace.VALUE` and `CompactResolveUtil.Namespace.TYPE` handling.
4. **Tolerance for incomplete code**: Guard all PSI accesses, inspections, structure elements, doc providers, and formatting routines against `null` and `PsiErrorElement` nodes.
5. **Do not invent Compact language semantics**: Verify all syntax and typing rules against official compiler references (`compact/compiler/` and `.ai/context/compact-semantics.md`).
6. **Preserve existing tests**: All unit tests must pass before finishing any feature (`./gradlew test`). Current verified baseline is 472 unit tests.
7. **Inspect before modifying**: Read targeted production files before making code edits.
8. **`.gitignore` and Hidden Folders (`.ai/`, `compact/`, `intellij-*/`) are NOT `.aiignore`**: Standard tools (`grep_search` / `find_by_name`) automatically ignore hidden dot-folders and patterns in `.gitignore`. AI agents MUST NOT treat these files as excluded, ignored, or nonexistent. All files under `.ai/decisions/` (such as `ADR-001` through `ADR-009`), `.ai/context/`, `docs/`, and gitignored reference codebases (`compact/`, `intellij-elixir/`, `intellij-rust/`, `intellij-scala/`, `intellij-solidity/`, `Rplugin/`) are vital project assets that must be directly accessed via `client_view_file`, `view_file`, or un-ignored commands.
9. **MANDATORY Architecture Decision Records (ADRs) & Sourcing Rule**:
   - **Document Every Architectural Decision & Important Code Change**: Whenever an architectural decision is made, a language rule is formalized, or an important behavioral change is introduced (e.g. Smart Enter completion, commenter/doc scaffolding, intention scoping, type grammar alignment), an ADR **MUST** immediately be authored or updated in [`.ai/decisions/`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/) and registered in [`.ai/decisions/README.md`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/README.md).
   - **Ground Truth & Anti-Hardcoding Rigor**: Every ADR and implementation must strictly cite upstream ground truth (`compact/compiler/parser.ss`, `lexer.ss`, `langs.ss`, `midnight-ledger.ss`) and cross-verify with production reference plugins (`intellij-rust`, `intellij-scala`, `intellij-elixir`). Never accept brittle hardcoded heuristics when scalable grammar-driven models exist.
   - **Feature Implementation Mapping**: Every ADR must provide an explicit file and line map linking the decision to its production classes and test fixtures for instant discovery and future maintainability.
10. **MANDATORY Definition of Done (DoD) & Changelog Gate**:
   - **Never close a task without updating `CHANGELOG.md`**: Any feature, improvement, or bugfix MUST immediately be recorded under `## [Unreleased]` in [`CHANGELOG.md`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/CHANGELOG.md) following Keep-a-Changelog conventions.
   - **Synchronize `.ai/` state**: Update `.ai/context/current-state.md` and `.ai/project-state.yaml` with the latest passing test count and feature status whenever code changes or architecture decisions are completed.
   - **Zero Warnings**: Ensure `./gradlew test` passes 100% with zero failures and zero compiler warnings.
11. **Multi-Tier Testing Strategy (Production Standard)**:
   - **Tier 1 (Golden Tree Conformance)**: Every core syntax construct must be backed by a fixture test validating the full PSI tree node-by-node against golden `.txt` trees without errors.
   - **Tier 2 (Partial Parsing & Error Recovery)**: Every parser component must be tested with deliberately broken and incomplete syntax (e.g. missing semicolons, trailing commas, unclosed braces) ensuring `PsiErrorElement` is registered without dropping surrounding contract definitions or freezing the EDT.
   - **Tier 3 (Operator Precedence & Associativity Matrix)**: Test complex binary, bitwise, boolean, relational, and range expression trees to guarantee correct AST hierarchy.
   - **Tier 4 (Stress & Stack Resilience)**: Test resilience against deeply nested parentheses, brackets, and blocks (preventing `StackOverflowError` DoS).
   - **Tier 5 (Non-Blocking Concurrency)**: All inspections, external linters, and index operations must yield immediately upon cancellation without UI thread lag.

---

## 4. Modern Java (Java 25) Guidelines & Feature Utilization (MANDATORY)

Since the plugin builds against the Java 25 toolchain (`JavaLanguageVersion.of(25)`), developers and AI agents **MUST prefer modern Java features** over legacy, verbose pre-Java 17/21 patterns:

1. **Prefer Java Records**:
   - Use `record` for immutable data carriers, DTOs, type descriptors, parser/resolver return values, and cache keys instead of boilerplate classes with manual getters, `equals()`, `hashCode()`, and `toString()`.
2. **Pattern Matching for `switch` & Enhanced Switch Expressions**:
   - Use arrow switch expressions (`switch (...) ->`) yielding values or returning directly rather than legacy statement switches with fallthrough and `break;`.
   - Match types directly in switch branches (e.g. `case CompactCircuitDefinition circuit -> ...`).
3. **Unnamed Variables and Patterns (`_`) (JEP 456 / Java 22+)**:
   - When pattern matching types in `switch` or `instanceof`, handling unused lambda arguments, or handling exceptions where the bound variable is not consumed, **ALWAYS use the unnamed pattern `_`** (e.g. `case CompactCircuitDefinition _ -> ...`, `try { ... } catch (IOException _) { ... }`).
   - Never declare named pattern variables that remain unused and trigger compiler/IDE warnings (`Pattern variable 'xyz' is never used`).
4. **Simplify Conditionals and Predicates**:
   - Collapse boolean check ladders into direct boolean returns (`return (condA && condB) || (condC && condD);`) rather than writing multiple `if (...) return true; return false;`.
   - Avoid redundant case-insensitive checks (e.g., `"Void".equalsIgnoreCase(str)` already matches `"void"`, `"VOID"`, etc.).
   - Collapse nested `if` blocks into single compound conditions with `&&` or guard clauses.
5. **Record Patterns & Deconstruction (JEP 440)**:
   - Leverage nested record deconstruction in `instanceof` and `switch` where components are immediately accessed.
6. **Sequenced Collections & Modern Standard Library APIs**:
   - Use Sequenced Collections methods (`getFirst()`, `getLast()`, `reversed()`).
   - Use immutable collection factories (`List.of()`, `Set.of()`, `Map.of()`).
   - Use modern string methods (`String.isBlank()`, `String.strip()`, `String.repeat()`).
7. **Typography & Code Style**:
   - In Javadoc, code comments, and documentation, adhere to standard American English punctuation: place commas after introductory abbreviations such as `e.g., ` and `i.e., `.

---

## 5. Threading & Concurrency Rules (Quick Reference)

| Operation                      | Thread / Context                                 | Mechanism                                                                                          | Existing Repo Example                                                                                                                                                                     |
|:-------------------------------|:-------------------------------------------------|:---------------------------------------------------------------------------------------------------|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **PSI Read**                   | Background Thread or EDT with **ReadAction**     | Active in inspections, annotator `collectInformation` & `apply`; manual via `ReadAction.compute()` | [`CompactResolveUtil.resolve()`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/resolve/CompactResolveUtil.java)                                 |
| **PSI Mutation**               | **EDT only** with **WriteAction** & Command      | `WriteCommandAction.runWriteCommandAction(project, () -> ...)`                                     | [`CompactRemoveUnusedVariableFix.applyFix()`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/inspection/fix/CompactRemoveUnusedVariableFix.java) |
| **External Process Execution** | **Background Thread only**                       | `Task.Backgroundable`, `ExternalAnnotator.doAnnotate()`, `CommandLineState.startProcess()`         | [`CompactExternalAnnotator.doAnnotate()`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/annotator/CompactExternalAnnotator.java)                |
| **VFS Refresh**                | Any thread (**asynchronous** strongly preferred) | `VfsUtil.markDirtyAndRefresh(true, ...)`                                                           | Output directory refresh                                                                                                                                                                  |
| **UI Updates / Dialogs**       | **EDT only**                                     | `ApplicationManager.getApplication().invokeLater(...)`                                             | [`CompactCreateFileAction.buildDialog()`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/actions/CompactCreateFileAction.java)                   |
| **Service Access**             | Any thread                                       | `@Service` + `getInstance()`                                                                       | [`CompactStdlibService`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/stdlib/CompactStdlibService.java)                                        |

- **RULE 1**: **NEVER run `process.waitFor()` or external CLI operations on the EDT.** It freezes the entire IDE.
- **RULE 2**: **NEVER modify PSI on a background thread.** All AST deletions, replacements, and additions must be executed inside a `WriteCommandAction` on the EDT.
- **RULE 3**: **Always support cancellation.** In long background loops, call `ProgressManager.checkCanceled()` periodically.

---

## 6. Tool Selection Priorities: IntelliJ MCP vs. CLI (MANDATORY)

For Java codebase exploration, refactoring, and code verification within this project, agents **MUST prioritize the `idea` MCP server tools over raw CLI commands (`grep`, `find`, `cat`)**:

| Task                                   | Preferred MCP Tool (`idea` server via `call_mcp_tool`)                                                     | Replaces CLI Tool             | Reason                                                                                                              |
|:---------------------------------------|:-----------------------------------------------------------------------------------------------------------|:------------------------------|:--------------------------------------------------------------------------------------------------------------------|
| **Find Java Class / Method / Symbol**  | `execute_tool --command "search_symbol --q <Name>"`                                                        | `grep_search`, `find_by_name` | Queries IntelliJ's live index; returns exact start/end line without false matches or noise (~40 tokens).            |
| **Inspect Symbol Signature / JavaDoc** | `execute_tool --command "get_symbol_info --filePath <path> --line <L> --column <C>"`                       | `client_view_file`, `cat`     | Returns exact signature, inheritance (`extends`, `implements`), and JavaDoc without dumping hundreds of file lines. |
| **Check Compilation & Errors**         | `execute_tool --command "get_file_problems --filePath <path>"`                                             | `./gradlew compileJava`       | Instant (0s) check against IntelliJ's live error highlighter.                                                       |
| **Run Static Inspections**             | `execute_tool --command "lint_files --files [\"<path>\"]"`                                                 | CLI checkstyle                | Evaluates project inspections across target files.                                                                  |
| **Cross-File Renaming**                | `execute_tool --command "rename_refactoring --pathInProject <p> --symbolName <old> --newName <new>"`       | Manual search & replace       | True AST refactoring updating declarations, calls, imports, and overrides safely.                                   |
| **Code Formatting**                    | `execute_tool --command "reformat_file --files [\"<path>\"]"`                                              | Manual formatting             | Applies IntelliJ's exact code style settings.                                                                       |
| **Discovered Run Configurations**      | `execute_tool --command "get_run_configurations"` / `execute_run_configuration --configurationName <name>` | CLI command guessing          | Leverages configured IntelliJ run targets directly.                                                                 |

*Note: Use CLI/native tools (`client_view_file`, `run_command`) only for non-Java files (e.g. `.compact`, Markdown, YAML, Gradle configs) or if the IDE MCP server is unavailable or busy.*

---

## 7. Reference Repository Selection Matrix

> [!IMPORTANT]
> **MANDATORY RULE FOR COMPARISONS**: Whenever asked to compare with production repositories or reference implementations, ALWAYS compare against the four local reference repositories located directly in the workspace root:
> - **`intellij-elixir/`**: Handwritten Lexer/Parser, `LexerBase`, PSI infrastructure, and element factory.
> - **`intellij-scala/`**: Keyword completion scoping (`ScalaKeywordCompletionContributor`), compile server daemon, REPL.
> - **`intellij-rust/`**: Multi-level completion provider (`RsKeywordCompletionContributor` with `declarationPattern` vs `newCodeStatementPattern`), run configurations, `ExternalAnnotator`.
> - **`Rplugin/`**: WSL interpreter/toolchain discovery (`RToolchain`), Windows path translation, project generators.
>
> Never guess, generalize, or rely on hypothetical assumptions when these local production repositories are directly available to inspect.

| Subsystem                           | Best Reference Repository           | Key Reference Directory / File                                                 | What to Inspect                                             |
|:------------------------------------|:------------------------------------|:-------------------------------------------------------------------------------|:------------------------------------------------------------|
| **Compact Syntax & Semantics**      | `compact/`                          | `compact/compiler/parser.ss`, `standard-library.compact`                       | Official EBNF grammar, type rules, and standard library     |
| **Handwritten Lexer / Parser**      | `intellij-elixir/`                  | `intellij-elixir/src/org/elixir_lang/lexer/`                                   | `LexerBase` token stream handling and PSI integration       |
| **Keyword & Scope Completion**      | `intellij-rust/`, `intellij-scala/` | `RsKeywordCompletionContributor.kt`, `ScalaKeywordCompletionContributor.scala` | Context filtering (top-level vs statement vs expression)    |
| **Run Configurations & Toolchains** | `intellij-rust/`                    | `intellij-rust/.../cargo/runconfig/`                                           | Context-aware run producers and gutter play actions         |
| **External Linter Annotator**       | `intellij-rust/`                    | `intellij-rust/.../ide/annotator/`                                             | 3-phase `ExternalAnnotator` pipeline and diagnostic parser  |
| **Toolchain & WSL Path Discovery**  | `Rplugin/`                          | `Rplugin/psi/.../interpreter/`                                                 | WSL distribution discovery and Windows path translation     |
| **Project Generators & Wizards**    | `Rplugin/`                          | `Rplugin/src/.../projectGenerator/`                                            | `DirectoryProjectGenerator` and step UI templates           |
| **Compiler Daemons & REPL**         | `intellij-scala/`                   | `intellij-scala/scala/compile-server/`, `scala/repl/`                          | Socket IPC, background build servers, and console execution |
| **Local Devnet & Node RPC**         | `../midnight-local-dev/`            | `standalone.yml`, `accounts.json`                                              | Proof server `6300`, Node RPC `9944`, dev accounts          |

---

## 8. Critical Pitfalls & Anti-Patterns to Avoid

1. **The Windows `compact.exe` Trap**: Windows has a native NTFS compression utility at `C:\Windows\System32\compact.exe`. Never execute `findExecutableInPath("compact")` on Windows without prioritizing WSL and filtering out Windows system directories. Use [`CompactToolchainUtil`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/run/CompactToolchainUtil.java).
2. **Velocity `${NAME}` Pollution**: In file templates, the `${NAME}` property must be the pure simple identifier (e.g. `Token`), not a file path (`sub/Token`) or file name (`Token.compact`).
3. **Bypassing `CreateFileAction.MkDirs`**: When creating files from templates with paths, always use `MkDirs` to create intermediate directories; calling `dir.createFile()` with path slashes throws `IncorrectOperationException`.
4. **Missing `<internalFileTemplate>`**: In modern IntelliJ Platform, bundled file templates (`.ft`) must be explicitly declared in `plugin.xml` with `<internalFileTemplate name="..."/>` or IntelliJ's usage statistics collector throws assertion errors in tests.
5. **Merging Value & Type Namespaces**: Compact has distinct namespaces. Resolving `Point` in an expression must never resolve to `struct Point` (type), and resolving `Point` in a type signature must never resolve to `const Point` (variable).
6. **Swallowing Exceptions**: Never use empty `catch (Exception e) { return null; }` blocks that mask genuine configuration errors or permissions issues from the user.
7. **Memory Leaks via Static PSI**: Never store `PsiElement`, `PsiFile`, or `Project` instances in static fields, long-lived caches, or non-disposable listeners. Use `CachedValuesManager` or `Disposer`.

---

## 9. Layered Context System

Deconstructive context files are located in `.ai/`:

| Topic                 | Context File                       | Description                                                                                                                                                   |
|:----------------------|:-----------------------------------|:--------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Machine State**     | `.ai/project-state.yaml`           | Concise, machine-readable feature and test status                                                                                                             |
| **Architecture**      | `.ai/context/architecture.md`      | Detailed subsystem design, threading model, and extension points                                                                                              |
| **Current State**     | `.ai/context/current-state.md`     | Implementation snapshot, test breakdown, known limitations                                                                                                    |
| **Compact Semantics** | `.ai/context/compact-semantics.md` | Verified Compact language behavior and typing rules                                                                                                           |
| **Reference Map**     | `.ai/context/reference-map.md`     | Index of compiler (`compact/`), testnet (`midnight-local-dev/`), and IDE reference code (`intellij-rust/`, `intellij-elixir/`, `intellij-scala/`, `Rplugin/`) |
| **IntelliJ Patterns** | `.ai/context/intellij-patterns.md` | Plugin-specific IntelliJ Platform patterns, APIs, and comparative analysis                                                                                    |
| **Decisions (ADRs)**  | `.ai/decisions/`                   | Architectural Decision Records (ADR-001 through ADR-009)                                                                                                      |
| **Latest Handoff**    | `.ai/handoff.md`                   | Session-to-session continuation summary                                                                                                                       |

---

## 10. Context Loading Workflows

- **For planning a new feature**:
  1. Read `AGENTS.md` + `.ai/project-state.yaml` + `.ai/context/current-state.md`.
  2. Consult `.ai/context/architecture.md` for existing subsystem boundaries.
  3. Load targeted entries from `.ai/context/compact-semantics.md` and `.ai/context/reference-map.md`.
- **For implementing a planned feature**:
  1. Inspect only the relevant production and test packages in `src/` using MCP `search_symbol` and `get_symbol_info`.
  2. Implement code adhering to `.ai/context/intellij-patterns.md` and Modern Java guidelines.
  3. Verify file health via MCP `get_file_problems`.
  4. Run `./gradlew test` to verify zero regressions.
  5. Update `.ai/context/current-state.md` and `.ai/handoff.md`.
- **For language-semantic questions**:
  1. Check `.ai/context/compact-semantics.md`.
  2. If unknown, use `.ai/context/reference-map.md` to pinpoint 1–2 reference files in `compact/compiler/` and inspect targeted lines.
- **For IntelliJ platform questions**:
  1. Consult `.ai/context/intellij-patterns.md` and referenced patterns in `intellij-rust/`, `intellij-elixir/`, `intellij-scala/`, or `Rplugin/`.
- **For local devnet & network node questions**:
  1. Consult `../midnight-local-dev/` for Docker Compose configurations, RPC endpoints, and contract deployment scripts.

---

## 11. Context Efficiency & Token Rules

1. **Semantic lookups first**: Prefer MCP `search_symbol` and `get_symbol_info` for Java navigation.
2. **Targeted lookups**: Search before opening large non-Java files; use line-range slices for large sources.
3. **No bulk loading**: Never read entire reference repositories (`compact/`, `intellij-rust/`, `intellij-elixir/`, `intellij-scala/`, `Rplugin/`, `midnight-local-dev/`) into context.
4. **No duplicate documentation**: Update existing `.ai/` context files rather than creating overlapping notes.
5. **Summaries over raw code**: Extract verified rules into `compact-semantics.md` rather than pasting raw Scheme/Rust/Scala snippets.
6. **Exact references**: Use precise symbol and class names (`dev.verloren.midnight.psi.impl.CompactReferenceExprImpl`).

---

## 12. Code Review & Improvement Bar (Zero Improvement Hallucinations)

When the user asks open-ended, casual, or vague questions such as *"Does this file need improvements?"*, *"Can this be improved?"*, or *"Review this file"*:

1. **Default Verdict is "No Changes Needed"**:
   - The default and expected outcome for tested, working code is: **"No improvements required; this code is solid and production-ready."**
   - Do NOT treat review questions as an obligation or challenge to invent diffs or find something to modify.

2. **Strict Invariant Benchmark**:
   Only propose an improvement if there is a concrete, verifiable failure against one of these four criteria:
   - **Threading & Concurrency (Section 5)**: e.g., PSI read off ReadAction, PSI mutation off EDT/WriteCommandAction, or blocking `process.waitFor()` on the EDT.
   - **Critical Invariants (Section 3)**: e.g., Merged value/type namespaces, missing null/`PsiErrorElement` guards, or memory leaks via static PSI references.
   - **Language / Compiler Semantics**: Direct deviation from official compiler rules (`compact/compiler/` or `.ai/context/compact-semantics.md`).
   - **Correctness / Regression**: A tangible bug, broken test, or resource leak.

3. **Strictly Prohibited Proposals (Anti-Bikeshedding)**:
   - **NO cosmetic rewrites**: Do not convert working loops to streams, reformat working code, or swap functional vs. imperative style.
   - **NO speculative abstractions**: Do not invent interfaces, factories, or builder patterns where direct implementations already work.
   - **NO subjective renames**: Do not suggest renaming local variables or methods that already follow repository conventions.
   - **NO rewriting working architecture**: The parser, lexer, resolver, annotator, and PSI wrappers are verified with 472 passing tests.

4. **Output Format when Clean**:
   If none of the four criteria are violated, state concisely:
   > **"No improvements required."** Followed by a 1–2 bullet summary confirming compliance with the relevant invariants (e.g., threading model, null-safety, test coverage).

---

## 13. Code Update & Release Protocol (What to Update on Every Change)

To ensure consistency, documentation integrity, and smooth JetBrains Marketplace releases, follow this checklist whenever modifying code:

### 1. On Every Feature, Fix, or Code Update
- **Code & Threading Safety**:
  - Strictly adhere to Threading Rules (Section 5) and Critical Invariants (Section 3).
  - Run `./gradlew test` and confirm all unit tests pass with zero regressions (current baseline: 472 tests across 55 test suites).
- **Changelog ([`CHANGELOG.md`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/CHANGELOG.md))**:
  - Always record changes under `## [Unreleased]` using Keep a Changelog categories:
    - `### Added` for new IDE features, inspections, templates, or compiler support.
    - `### Changed` for modified behaviors or API migrations.
    - `### Fixed` for bug fixes, test corrections, or edge-case handling.
- **Architectural Decisions ([`.ai/decisions/`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/))**:
  - For any architectural decision or important code change, add/update an ADR in `.ai/decisions/` and update `.ai/decisions/README.md`.
- **AI Context Files (`.ai/`)**:
  - Update [`.ai/context/current-state.md`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/current-state.md) with updated phase descriptions and test metrics.
  - Update [`.ai/handoff.md`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/.ai/handoff.md) with current status, implementation notes, and next priorities.
  - Update [`.ai/project-state.yaml`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/.ai/project-state.yaml) if feature phase status or test suite counts change.

### 2. When User-Facing Features or Setup Instructions Change
- **Plugin Description ([`plugin.xml`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/src/main/resources/META-INF/plugin.xml))**:
  - If new inspections, toolchain configurations, settings, or major capabilities are added, update the `<description>` CDATA block so the Marketplace page and IDE **Settings → Plugins** details tab stay accurate.

### 3. When Releasing / Cutting a New Version
- **Bump Version**: Update `version` in [`gradle.properties`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/gradle.properties) (e.g. `1.2.4`).
- **Version the Changelog**: In [`CHANGELOG.md`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/CHANGELOG.md), move the completed bullets from `## [Unreleased]` into a new version header (e.g. `## [1.2.4]`).
- **Sync XML Change Notes**: Update the static fallback `<change-notes>` block in [`src/main/resources/META-INF/plugin.xml`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/src/main/resources/META-INF/plugin.xml) to mirror the new version's changelog.
- **Build & Verify Distribution**:
  - Run `./gradlew buildPlugin` to verify compilation, test passes, and distribution zip packaging in `build/distributions/`.
