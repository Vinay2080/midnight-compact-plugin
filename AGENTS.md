# AI & Developer Instructions — Midnight Language Plugin

## 1. Project Identity
- **Project**: Midnight Compact Language Plugin for IntelliJ IDEA (`dev.verloren.midnight`).
- **Purpose**: First-class development support for the Midnight blockchain's Compact smart contract language.
- **Language**: Java 17+ (IntelliJ Platform Gradle Plugin).
- **Core Status**: Lexer, Parser, PSI, References, Completion, Refactoring, Find Usages, Type Inference, Inspections, Formatter, Smart Indentation, Structure View, Documentation Provider, Cross-File Resolution, File/Live Templates, Folding, Breadcrumbs, Run Configurations, External Annotator, Line Markers, and Bundled Stdlib are implemented and verified (376/376 unit tests passing).

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
[IDE Features] Completion, Rename, Find Usages, Formatter, Smart Indent, Structure View, Docs, Run Configurations, External Linter Annotator, Line Markers
```

---

## 3. Critical Invariants

1. **Do not replace working architecture**: The handwritten lexer, parser, PSI wrappers, resolver, structure view, and docs provider are mature and verified. Do not replace them with generated parsers or external tools without an explicit directive.
2. **Reuse existing PSI and resolve infrastructure**: Always use `CompactResolveUtil` for symbol lookups and `CompactElementFactory` for PSI node generation.
3. **Strict namespace separation**: Maintain distinct `CompactResolveUtil.Namespace.VALUE` and `CompactResolveUtil.Namespace.TYPE` handling.
4. **Tolerance for incomplete code**: Guard all PSI accesses, inspections, structure elements, doc providers, and formatting routines against `null` and `PsiErrorElement` nodes.
5. **Do not invent Compact language semantics**: Verify all syntax and typing rules against official compiler references (`compact/compiler/` and `.ai/context/compact-semantics.md`).
6. **Preserve existing tests**: All 376 unit tests must pass before finishing any feature (`./gradlew test`).
7. **Inspect before modifying**: Read targeted production files before making code edits.

---

## 4. Threading & Concurrency Rules (Quick Reference)

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

## 5. Reference Repository Selection Matrix

| Subsystem                           | Best Reference Repository | Key Reference Directory / File                           | What to Inspect                                             |
|:------------------------------------|:--------------------------|:---------------------------------------------------------|:------------------------------------------------------------|
| **Compact Syntax & Semantics**      | `compact/`                | `compact/compiler/parser.ss`, `standard-library.compact` | Official EBNF grammar, type rules, and standard library     |
| **Handwritten Lexer / Parser**      | `intellij-elixir/`        | `intellij-elixir/src/org/elixir_lang/lexer/`             | `LexerBase` token stream handling and PSI integration       |
| **Run Configurations & Toolchains** | `intellij-rust/`          | `intellij-rust/.../cargo/runconfig/`                     | Context-aware run producers and gutter play actions         |
| **External Linter Annotator**       | `intellij-rust/`          | `intellij-rust/.../ide/annotator/`                       | 3-phase `ExternalAnnotator` pipeline and diagnostic parser  |
| **Toolchain & WSL Path Discovery**  | `Rplugin/`                | `Rplugin/psi/.../interpreter/`                           | WSL distribution discovery and Windows path translation     |
| **Project Generators & Wizards**    | `Rplugin/`                | `Rplugin/src/.../projectGenerator/`                      | `DirectoryProjectGenerator` and step UI templates           |
| **Compiler Daemons & REPL**         | `intellij-scala/`         | `intellij-scala/scala/compile-server/`, `scala/repl/`    | Socket IPC, background build servers, and console execution |
| **Local Devnet & Node RPC**         | `../midnight-local-dev/`  | `standalone.yml`, `accounts.json`                        | Proof server `6300`, Node RPC `9944`, dev accounts          |

---

## 6. Critical Pitfalls & Anti-Patterns to Avoid

1. **The Windows `compact.exe` Trap**: Windows has a native NTFS compression utility at `C:\Windows\System32\compact.exe`. Never execute `findExecutableInPath("compact")` on Windows without prioritizing WSL and filtering out Windows system directories. Use [`CompactToolchainUtil`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/run/CompactToolchainUtil.java).
2. **Velocity `${NAME}` Pollution**: In file templates, the `${NAME}` property must be the pure simple identifier (e.g. `Token`), not a file path (`sub/Token`) or file name (`Token.compact`).
3. **Bypassing `CreateFileAction.MkDirs`**: When creating files from templates with paths, always use `MkDirs` to create intermediate directories; calling `dir.createFile()` with path slashes throws `IncorrectOperationException`.
4. **Missing `<internalFileTemplate>`**: In modern IntelliJ Platform, bundled file templates (`.ft`) must be explicitly declared in `plugin.xml` with `<internalFileTemplate name="..."/>` or IntelliJ's usage statistics collector throws assertion errors in tests.
5. **Merging Value & Type Namespaces**: Compact has distinct namespaces. Resolving `Point` in an expression must never resolve to `struct Point` (type), and resolving `Point` in a type signature must never resolve to `const Point` (variable).
6. **Swallowing Exceptions**: Never use empty `catch (Exception e) { return null; }` blocks that mask genuine configuration errors or permissions issues from the user.
7. **Memory Leaks via Static PSI**: Never store `PsiElement`, `PsiFile`, or `Project` instances in static fields, long-lived caches, or non-disposable listeners. Use `CachedValuesManager` or `Disposer`.

---

## 7. Layered Context System

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

## 8. Context Loading Workflows

- **For planning a new feature**:
  1. Read `AGENTS.md` + `.ai/project-state.yaml` + `.ai/context/current-state.md`.
  2. Consult `.ai/context/architecture.md` for existing subsystem boundaries.
  3. Load targeted entries from `.ai/context/compact-semantics.md` and `.ai/context/reference-map.md`.
- **For implementing a planned feature**:
  1. Inspect only the relevant production and test packages in `src/`.
  2. Implement code adhering to `.ai/context/intellij-patterns.md`.
  3. Run `./gradlew test` to verify zero regressions.
  4. Update `.ai/context/current-state.md` and `.ai/handoff.md`.
- **For language-semantic questions**:
  1. Check `.ai/context/compact-semantics.md`.
  2. If unknown, use `.ai/context/reference-map.md` to pinpoint 1–2 reference files in `compact/compiler/` and inspect targeted lines.
- **For IntelliJ platform questions**:
  1. Consult `.ai/context/intellij-patterns.md` and referenced patterns in `intellij-rust/`, `intellij-elixir/`, `intellij-scala/`, or `Rplugin/`.
- **For local devnet & network node questions**:
  1. Consult `../midnight-local-dev/` for Docker Compose configurations, RPC endpoints, and contract deployment scripts.

---

## 9. Context Efficiency & Token Rules

1. **Targeted lookups**: Search before opening large files; use line-range slices for large sources.
2. **No bulk loading**: Never read entire reference repositories (`compact/`, `intellij-rust/`, `intellij-elixir/`, `intellij-scala/`, `Rplugin/`, `midnight-local-dev/`) into context.
3. **No duplicate documentation**: Update existing `.ai/` context files rather than creating overlapping notes.
4. **Summaries over raw code**: Extract verified rules into `compact-semantics.md` rather than pasting raw Scheme/Rust/Scala snippets.
5. **Exact references**: Use precise symbol and class names (`dev.verloren.midnight.psi.impl.CompactReferenceExprImpl`).
