# AI & Developer Instructions — Midnight Language Plugin

## 1. Project Identity
- **Project**: Midnight Compact Language Plugin for IntelliJ IDEA (`dev.verloren.midnight`).
- **Purpose**: First-class development support for the Midnight blockchain's Compact smart contract language.
- **Language**: Java 25 (Gradle toolchain `JavaLanguageVersion.of(25)`).
- **Core Status**: Lexer, Parser, PSI, References, Completion, Refactoring, Find Usages, Type Inference, Inspections, Formatter, Smart Indentation, Structure View, Documentation Provider, Cross-File Resolution, File/Live Templates, Folding, Breadcrumbs, Run Configurations, External Annotator, Line Markers, Multi-Version Compiler Management, Remix-Style Compiler Tool Window, Pragma Quick-Fixes, and Bundled Stdlib are implemented and verified (389 unit tests passing across forty-seven test suites).

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
[IDE Features] Completion, Rename, Find Usages, Formatter, Smart Indent, Structure View, Docs, Run Configurations, External Linter Annotator, Line Markers, Multi-Version Toolchain & Remix Compiler Tool Window
```

---

## 3. Critical Invariants

1. **Do not replace working architecture**: The handwritten lexer, parser, PSI wrappers, resolver, structure view, and docs provider are mature and verified. Do not replace them with generated parsers or external tools without an explicit directive.
2. **Reuse existing PSI and resolve infrastructure**: Always use `CompactResolveUtil` for symbol lookups and `CompactElementFactory` for PSI node generation.
3. **Strict namespace separation**: Maintain distinct `CompactResolveUtil.Namespace.VALUE` and `CompactResolveUtil.Namespace.TYPE` handling.
4. **Tolerance for incomplete code**: Guard all PSI accesses, inspections, structure elements, doc providers, and formatting routines against `null` and `PsiErrorElement` nodes.
5. **Do not invent Compact language semantics**: Verify all syntax and typing rules against official compiler references (`compact/compiler/` and `.ai/context/compact-semantics.md`).
6. **Preserve existing tests**: All 389 unit tests must pass before finishing any feature (`./gradlew test`).
7. **Inspect before modifying**: Read targeted production files before making code edits.

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
| **Decisions (ADRs)**  | `.ai/decisions/`                   | Architectural Decision Records (ADR-001 through ADR-005)                                                                                                      |
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
