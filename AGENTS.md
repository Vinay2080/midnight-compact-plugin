# AI & Developer Instructions — Midnight Language Plugin

## 1. Project Identity & Standards
- **Project**: Midnight Compact Language Plugin for IntelliJ IDEA (`dev.verloren.midnight`).
- **Purpose**: First-class development support for the Midnight blockchain's Compact smart contract language.
- **Language**: Java 25 (Gradle toolchain `JavaLanguageVersion.of(25)`).
- **Architecture & System Status**: For current implementation state, active phases, and verified test metrics, consult [`.ai/project-state.yaml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/project-state.yaml) and [`.ai/context/current-state.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/current-state.md).
- **Execution & Workflow**: Every task must follow the mandatory task lifecycle, documentation decision rules, and completion gate defined in [`.ai/workflow.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/workflow.md).

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

1. **Do Not Replace Working Architecture**: The handwritten lexer, parser, PSI wrappers, resolver, structure view, and docs provider are mature and verified. Do not replace them with generated parsers or external tools without an explicit directive.
2. **Reuse Existing PSI and Resolve Infrastructure**: Always use [`CompactResolveUtil`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/resolve/CompactResolveUtil.java) for symbol lookups and [`CompactElementFactory`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/psi/CompactElementFactory.java) for PSI node generation.
3. **Strict Namespace Separation**: Maintain distinct `CompactResolveUtil.Namespace.VALUE` and `CompactResolveUtil.Namespace.TYPE` handling. A type and a variable can share an identifier without collision.
4. **Tolerance for Incomplete Code**: Guard all PSI accesses, inspections, structure elements, doc providers, and formatting routines against `null` and `PsiErrorElement` nodes. Parser loops must guarantee token advancement on every iteration to prevent UI thread freezes.
5. **Do Not Invent Compact Language Semantics**: Verify all syntax and typing rules against official compiler references (`compact/compiler/` and [`.ai/context/compact-semantics.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/compact-semantics.md)).
6. **Zero Test Regressions & Zero Warnings**: All unit tests must pass before completing any task (`./gradlew test`). Compilations and test suites must complete with zero failures and zero warnings.
7. **Inspect Before Modifying**: Read targeted production and test files before making code edits. Never propose changes based on unverified assumptions.
8. **`.gitignore` and Dot-Folders Are NOT `.aiignore`**: Standard search tools (`grep_search`, `find_by_name`) automatically ignore hidden dot-folders and patterns in `.gitignore`. AI agents MUST NOT treat these files as excluded or nonexistent. All files under [`.ai/decisions/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/), [`.ai/context/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/), `docs/`, and reference codebases (`compact/`, `intellij-elixir/`, `intellij-rust/`, `intellij-scala/`, `Rplugin/`) are vital project assets that must be directly accessed via `client_view_file` or `view_file`.
9. **Ground Truth & Anti-Hardcoding Rigor**: Every architectural decision and implementation must cite upstream ground truth (`compact/compiler/parser.ss`, `lexer.ss`, `langs.ss`, `midnight-ledger.ss`) and cross-verify with production reference plugins (`intellij-rust`, `intellij-scala`, `intellij-elixir`, `Rplugin`). Never accept brittle hardcoded heuristics when scalable grammar-driven models exist.
10. **Multi-Tier Testing Standard**:
    - **Tier 1 (Golden Tree Conformance)**: Validate full PSI tree node-by-node against golden `.txt` trees without errors.
    - **Tier 2 (Partial Parsing & Error Recovery)**: Test with deliberately broken syntax (missing semicolons, unclosed braces) ensuring `PsiErrorElement` is registered without freezing EDT.
    - **Tier 3 (Operator Precedence & Associativity Matrix)**: Test complex binary, bitwise, boolean, relational, and range expression trees to guarantee correct AST hierarchy.
    - **Tier 4 (Stress & Stack Resilience)**: Test resilience against deeply nested parentheses, brackets, and blocks (preventing `StackOverflowError`).
    - **Tier 5 (Non-Blocking Concurrency)**: All inspections, linters, and index operations must yield immediately upon cancellation without UI thread lag.
11. **Mandatory Task Lifecycle & Completion Gate**: Every change must adhere to the lifecycle in [`.ai/workflow.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/workflow.md), including documentation decision rules, anti-assumption re-read verification, and the final 9-step completion gate.

---

## 4. Modern Java (Java 25) Guidelines & Feature Utilization

The plugin builds against Java 25 (`JavaLanguageVersion.of(25)`). Developers and AI agents **MUST prefer modern Java features** over legacy, verbose pre-Java 17/21 patterns:

1. **Prefer Java Records**:
   - Use `record` for immutable data carriers, DTOs, type descriptors, parser/resolver return values, and cache keys instead of boilerplate classes with manual getters, `equals()`, `hashCode()`, and `toString()`.
2. **Pattern Matching for `switch` & Enhanced Switch Expressions**:
   - Use arrow switch expressions (`switch (...) ->`) yielding values or returning directly rather than legacy statement switches with fallthrough and `break;`.
   - Match types directly in switch branches (e.g. `case CompactCircuitDefinition circuit -> ...`).
3. **Unnamed Variables and Patterns (`_`) (JEP 456 / Java 22+)**:
   - When pattern matching types in `switch` or `instanceof`, handling unused lambda arguments, or handling exceptions where the bound variable is not consumed, **ALWAYS use the unnamed pattern `_`** (e.g. `case CompactCircuitDefinition _ -> ...`, `try { ... } catch (IOException _) { ... }`).
   - Never declare named pattern variables that remain unused and trigger compiler/IDE warnings.
4. **Simplify Conditionals and Predicates**:
   - Collapse boolean check ladders into direct boolean returns (`return (condA && condB) || (condC && condD);`) rather than writing multiple `if (...) return true; return false;`.
   - Avoid redundant case-insensitive checks (`"Void".equalsIgnoreCase(str)` matches `"void"`, `"VOID"`, etc.).
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

## 5. Threading & Concurrency Rules

| Operation | Thread / Context | Mechanism | Existing Repo Example |
|:---|:---|:---|:---|
| **PSI Read** | Background Thread or EDT with **ReadAction** | Active in inspections, annotator `collectInformation` & `apply`; manual via `ReadAction.compute()` | [`CompactResolveUtil.resolve()`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/resolve/CompactResolveUtil.java) |
| **PSI Mutation** | **EDT only** with **WriteAction** & Command | `WriteCommandAction.runWriteCommandAction(project, () -> ...)` | [`CompactRemoveUnusedVariableFix.applyFix()`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/inspection/fix/CompactRemoveUnusedVariableFix.java) |
| **External Process Execution** | **Background Thread only** | `Task.Backgroundable`, `ExternalAnnotator.doAnnotate()`, `CommandLineState.startProcess()` | [`CompactExternalAnnotator.doAnnotate()`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/annotator/CompactExternalAnnotator.java) |
| **VFS Refresh** | Any thread (**asynchronous** strongly preferred) | `VfsUtil.markDirtyAndRefresh(true, ...)` | Output directory refresh |
| **UI Updates / Dialogs** | **EDT only** | `ApplicationManager.getApplication().invokeLater(...)` | [`CompactCreateFileAction.buildDialog()`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/actions/CompactCreateFileAction.java) |
| **Service Access** | Any thread | `@Service` + `getInstance()` | [`CompactStdlibService`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/stdlib/CompactStdlibService.java) |

- **RULE 1**: **NEVER run `process.waitFor()` or external CLI operations on the EDT.** It freezes the entire IDE.
- **RULE 2**: **NEVER modify PSI on a background thread.** All AST deletions, replacements, and additions must be executed inside a `WriteCommandAction` on the EDT.
- **RULE 3**: **Always support cancellation.** In long background loops, call `ProgressManager.checkCanceled()` periodically.

---

## 6. Tool Selection Priorities: IntelliJ MCP vs. CLI

For Java codebase exploration, refactoring, and code verification within this project, agents **MUST prioritize the `idea` MCP server tools over raw CLI commands (`grep`, `find`, `cat`)**:

| Task | Preferred MCP Tool (`idea` server via `call_mcp_tool`) | Replaces CLI Tool | Reason |
|:---|:---|:---|:---|
| **Find Java Class / Method / Symbol** | `execute_tool --command "search_symbol --q <Name>"` | `grep_search`, `find_by_name` | Queries IntelliJ's live index; returns exact start/end line without false matches or noise (~40 tokens). |
| **Inspect Symbol Signature / JavaDoc** | `execute_tool --command "get_symbol_info --filePath <path> --line <L> --column <C>"` | `client_view_file`, `cat` | Returns exact signature, inheritance (`extends`, `implements`), and JavaDoc without dumping hundreds of file lines. |
| **Check Compilation & Errors** | `execute_tool --command "get_file_problems --filePath <path>"` | `./gradlew compileJava` | Instant (0s) check against IntelliJ's live error highlighter. |
| **Run Static Inspections** | `execute_tool --command "lint_files --files [\"<path>\"]"` | CLI checkstyle | Evaluates project inspections across target files. |
| **Cross-File Renaming** | `execute_tool --command "rename_refactoring --pathInProject <p> --symbolName <old> --newName <new>"` | Manual search & replace | True AST refactoring updating declarations, calls, imports, and overrides safely. |
| **Code Formatting** | `execute_tool --command "reformat_file --files [\"<path>\"]"` | Manual formatting | Applies IntelliJ's exact code style settings. |
| **Discovered Run Configurations** | `execute_tool --command "get_run_configurations"` / `execute_run_configuration --configurationName <name>` | CLI command guessing | Leverages configured IntelliJ run targets directly. |

*Note: Use CLI/native tools (`client_view_file`, `run_command`) for non-Java files (`.compact`, Markdown, YAML, Gradle configs) or if the IDE MCP server is unavailable.*

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

| Subsystem | Best Reference Repository | Key Reference Directory / File | What to Inspect |
|:---|:---|:---|:---|
| **Compact Syntax & Semantics** | `compact/` | `compact/compiler/parser.ss`, `standard-library.compact` | Official EBNF grammar, type rules, and standard library |
| **Handwritten Lexer / Parser** | `intellij-elixir/` | `intellij-elixir/src/org/elixir_lang/lexer/` | `LexerBase` token stream handling and PSI integration |
| **Keyword & Scope Completion** | `intellij-rust/`, `intellij-scala/` | `RsKeywordCompletionContributor.kt`, `ScalaKeywordCompletionContributor.scala` | Context filtering (top-level vs statement vs expression) |
| **Run Configurations & Toolchains** | `intellij-rust/` | `intellij-rust/.../cargo/runconfig/` | Context-aware run producers and gutter play actions |
| **External Linter Annotator** | `intellij-rust/` | `intellij-rust/.../ide/annotator/` | 3-phase `ExternalAnnotator` pipeline and diagnostic parser |
| **Toolchain & WSL Path Discovery** | `Rplugin/` | `Rplugin/psi/.../interpreter/` | WSL distribution discovery and Windows path translation |
| **Project Generators & Wizards** | `Rplugin/` | `Rplugin/src/.../projectGenerator/` | `DirectoryProjectGenerator` and step UI templates |
| **Compiler Daemons & REPL** | `intellij-scala/` | `intellij-scala/scala/compile-server/`, `scala/repl/` | Socket IPC, background build servers, and console execution |
| **Local Devnet & Node RPC** | `../midnight-local-dev/` | `standalone.yml`, `accounts.json` | Proof server `6300`, Node RPC `9944`, dev accounts |

---

## 8. Critical Pitfalls & Anti-Patterns to Avoid

1. **The Windows `compact.exe` Trap**: Windows has a native NTFS compression utility at `C:\Windows\System32\compact.exe`. Never execute `findExecutableInPath("compact")` on Windows without prioritizing WSL and filtering out Windows system directories. Use [`CompactToolchainUtil`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/run/CompactToolchainUtil.java).
2. **Velocity `${NAME}` Pollution**: In file templates, the `${NAME}` property must be the pure simple identifier (e.g. `Token`), not a file path (`sub/Token`) or file name (`Token.compact`).
3. **Bypassing `CreateFileAction.MkDirs`**: When creating files from templates with paths, always use `MkDirs` to create intermediate directories; calling `dir.createFile()` with path slashes throws `IncorrectOperationException`.
4. **Missing `<internalFileTemplate>`**: In modern IntelliJ Platform, bundled file templates (`.ft`) must be explicitly declared in `plugin.xml` with `<internalFileTemplate name="..."/>` or IntelliJ's usage statistics collector throws assertion errors in tests.
5. **Merging Value & Type Namespaces**: Compact has distinct namespaces. Resolving `Point` in an expression must never resolve to `struct Point` (type), and resolving `Point` in a type signature must never resolve to `const Point` (variable).
6. **Swallowing Exceptions**: Never use empty `catch (Exception e) { return null; }` blocks that mask genuine configuration errors or permissions issues from the user.
7. **Memory Leaks via Static PSI**: Never store `PsiElement`, `PsiFile`, or `Project` instances in static fields, long-lived caches, or non-disposable listeners. Use `CachedValuesManager` or `Disposer`.

---

## 9. Code Review & Improvement Bar (Zero Improvement Hallucinations)

When the user asks open-ended, casual, or vague questions such as *"Does this file need improvements?"*, *"Can this be improved?"*, or *"Review this file"*:

1. **Default Verdict is "No Changes Needed"**:
   - The default and expected outcome for tested, working code is: **"No improvements required; this code is solid and production-ready."**
   - Do NOT treat review questions as an obligation or challenge to invent diffs or find something to modify.

2. **Strict Invariant Benchmark**:
   Only propose an improvement if there is a concrete, verifiable failure against one of these four criteria:
   - **Threading & Concurrency (Section 5)**: e.g. PSI read off ReadAction, PSI mutation off EDT/WriteCommandAction, or blocking `process.waitFor()` on the EDT.
   - **Critical Invariants (Section 3)**: e.g. Merged value/type namespaces, missing null/`PsiErrorElement` guards, or memory leaks via static PSI references.
   - **Language / Compiler Semantics**: Direct deviation from official compiler rules (`compact/compiler/` or [`.ai/context/compact-semantics.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/compact-semantics.md)).
   - **Correctness / Regression**: A tangible bug, broken test, or resource leak.

3. **Strictly Prohibited Proposals (Anti-Bikeshedding)**:
   - **NO cosmetic rewrites**: Do not convert working loops to streams, reformat working code, or swap functional vs. imperative style.
   - **NO speculative abstractions**: Do not invent interfaces, factories, or builder patterns where direct implementations already work.
   - **NO subjective renames**: Do not suggest renaming local variables or methods that already follow repository conventions.
   - **NO rewriting working architecture**: The parser, lexer, resolver, annotator, and PSI wrappers are verified and production-ready.

4. **Output Format when Clean**:
   If none of the four criteria are violated, state concisely:
   > **"No improvements required."** Followed by a 1–2 bullet summary confirming compliance with the relevant invariants (e.g. threading model, null-safety, test coverage).

---

## 10. Context Navigation & Task Routing Map

All detailed context and workflow documents reside under [`.ai/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/):

| Need | Document | Description |
|:---|:---|:---|
| **Directory Index** | [`.ai/README.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/README.md) | Entry point & overview of the `.ai/` system |
| **Task Lifecycle & Gate** | [`.ai/workflow.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/workflow.md) | Standard 11-step workflow, decision rules, & final completion gate |
| **Machine State** | [`.ai/project-state.yaml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/project-state.yaml) | Machine-readable feature phases, test counts, and test suites |
| **Session Handoff** | [`.ai/handoff.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/handoff.md) | Current active feature, implementation status, and next priorities |
| **Architecture Guide** | [`.ai/context/architecture.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/architecture.md) | Detailed subsystem design, threading model, and extension points |
| **Current State** | [`.ai/context/current-state.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/current-state.md) | Narrative implementation snapshot and test breakdown |
| **Compact Semantics** | [`.ai/context/compact-semantics.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/compact-semantics.md) | Verified Compact language behavior, typing rules, and grammar |
| **IntelliJ Patterns** | [`.ai/context/intellij-patterns.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/intellij-patterns.md) | Plugin-specific IntelliJ Platform patterns, APIs, and comparative analysis |
| **Reference Map** | [`.ai/context/reference-map.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/reference-map.md) | Index of compiler (`compact/`) and reference plugins (`intellij-*/`, `Rplugin/`) |
| **Decisions (ADRs)** | [`.ai/decisions/README.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/README.md) | Master index of Architectural Decision Records (ADR-001 through ADR-026) |

### Task-Specific Context Loading Router
When starting a task, consult the **Context Loading Matrix** in [`.ai/workflow.md#2-phase-1-context-loading--task-routing`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/workflow.md) to load only the required files and avoid token bloat.
