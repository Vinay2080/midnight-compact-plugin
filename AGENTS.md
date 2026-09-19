# AI & Developer Instructions — Midnight Language Plugin

> [!CAUTION]
> ### 🛑 MANDATORY STEP 0: PRE-FLIGHT GUARD FOR ALL AGENTS
> **NEVER EDIT OR CREATE FILES DIRECTLY ON `master`.**
>
> Before executing ANY tool that modifies repository files (`client_edit_file`, `client_create_file`, file writes,
refactorings):
> 1. **VERIFY ISOLATION**:
>    - Check `git status` on `master`. Never overwrite, stash destructively, or discard existing user work.
    >    - Create and enter a dedicated worktree on a dedicated branch (`ai/<task-slug>`):
    >      - **Standard Worktree**: `git worktree add -b ai/<task-slug> ../midnight-plugin-wt-<task-slug> master`
        >      - **Sandbox / Tool-Restricted Worktree**: When operating inside environments where file tools are
                 restricted to the workspace root, use the gitignored worktree directory:
                 `git worktree add -b ai/<task-slug> .worktrees/<task-slug> master` (or switch to dedicated branch
                 `git checkout -b ai/<task-slug>`).
> 2. **STRICT ENFORCEMENT OF ALL CONSTRAINTS**:
     > Every rule in this document is **MANDATORY, CONTINUOUS, AND NON-NEGOTIABLE**:
>    - **Zero Direct-to-Master Edits**: All implementation, tests, docs, and `.ai/` updates must occur on the task
       branch before merging to `master`.
    >    - **Threading Strictness**: PSI reads strictly within `ReadAction`; PSI mutations strictly on EDT in
           `WriteCommandAction`; zero `process.waitFor()` on EDT.
    >    - **PSI / AST Robustness**: Guard all PSI accesses against `null` and `PsiErrorElement`. Parser loops must
           advance tokens on every step to prevent UI thread freezes.
    >    - **Dual Namespace Separation**: Never collide `CompactResolveUtil.Namespace.VALUE` and
           `CompactResolveUtil.Namespace.TYPE`.
    >    - **Comprehensive Modern Java up to Java 25**: Mandate the full spectrum of modern Java features (Java 14
           through Java 25: records, sequenced collections with `getFirst()`/`getLast()`, pattern matching for `switch`
           and `instanceof`, record deconstruction patterns, unnamed patterns `_`, sealed hierarchies, text blocks,
           modern streams, and immutable collection factories). Old pre-modern Java idioms are strictly forbidden.
    >    - **Mandatory Strict Inspection After Every Edit & Before Every Push**: After EVERY file edit and prior to
           pushing code/tags, run IntelliJ inspections (`get_file_problems` and `lint_files` via `execute_tool`).
           Resolve all reported problems: errors, warnings, weak warnings, grammar/spelling errors, and language-level
           change suggestions (e.g. Java 25 modernization, redundant casts/types), looping continuously until zero
           remain.
    >    - **Continuous Fix Loop**: Before doing work, during edits, after edits, and before release: whenever an issue
           is flagged or code is touched, fix it immediately. Never leave warnings, weak warnings, or suggestions
           unresolved.
    >    - **Compiler Ground Truth**: Never invent syntax or heuristics; verify against `compact/compiler/` and local
           reference plugins (`intellij-rust`, `intellij-elixir`, `intellij-scala`, `Rplugin`).
    >    - **Multi-Tier Testing**: Zero failures and zero compiler warnings on `./gradlew test`.
    >    - **Anti-Assumption Re-Read**: Always re-read edited files with `client_view_file` to verify edits before
           claiming task completion.
    >    - **Bug Knowledge Base**: Every concrete resolved defect MUST be documented in `.ai/bugs/` and indexed in
           `.ai/bugs/README.md`.
    >    - **User-Facing Changelog Hygiene vs. AI/Developer Knowledge**: [
           `CHANGELOG.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/CHANGELOG.md) is written strictly for
           plugin users and must remain clean and minimal; internal engineering details, AST mechanics, and compiler
           line citations belong exclusively in `.ai/`, git commits, and code comments.
    >    - **Semantic Tagging & Push Protocol**: Release tags (`vX.Y.Z`) must strictly reflect the nature of changes
           (Major, Minor, Patch). Never push code or tags without executing the release checklist and inspection gate
           defined in Section 12 and [
           `.ai/prompts/push-and-release.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/push-and-release.md).

---

## 1. Project Identity & Standards

- **Project**: Midnight Compact Language Plugin for IntelliJ IDEA (`dev.verloren.midnight`).
- **Purpose**: First-class development support for the Midnight blockchain's Compact smart contract language.
- **Language**: Java 25 (Gradle toolchain `JavaLanguageVersion.of(25)`).
- **Architecture & System Status**: For current implementation state, active phases, and verified test metrics,
  consult [`.ai/project-state.yaml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/project-state.yaml) and [
  `.ai/context/current-state.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/current-state.md). For
  persistent debugging records and prior defect investigations, consult [
  `.ai/bugs/README.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/bugs/README.md).
- **Execution & Workflow**: Every task must follow the mandatory task lifecycle, Git worktree isolation protocol,
  release-time changelog cleanup process, documentation decision rules, and completion gate defined in [
  `.ai/workflow.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/workflow.md).
- **Push & Release Automation**: For committing, inspecting, cleaning changelogs, tagging, and pushing code, execute the
  standardized custom prompt in [
  `.ai/prompts/push-and-release.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/push-and-release.md).

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

1. **Do Not Replace Working Architecture**: The handwritten lexer, parser, PSI wrappers, resolver, structure view, and
   docs provider are mature and verified. Do not replace them with generated parsers or external tools without an
   explicit directive.
2. **Reuse Existing PSI and Resolve Infrastructure**: Always use [
   `CompactResolveUtil`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/resolve/CompactResolveUtil.java)
   for symbol lookups and [
   `CompactElementFactory`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/psi/CompactElementFactory.java)
   for PSI node generation.
3. **Strict Namespace Separation**: Maintain distinct `CompactResolveUtil.Namespace.VALUE` and
   `CompactResolveUtil.Namespace.TYPE` handling. A type and a variable can share an identifier without collision.
4. **Tolerance for Incomplete Code**: Guard all PSI accesses, inspections, structure elements, doc providers, and
   formatting routines against `null` and `PsiErrorElement` nodes. Parser loops must guarantee token advancement on
   every iteration to prevent UI thread freezes.
5. **Do Not Invent Compact Language Semantics**: Verify all syntax and typing rules against official compiler references
   (`compact/compiler/` and [
   `.ai/context/compact-semantics.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/compact-semantics.md)).
6. **Zero Errors, Zero Warnings, Zero Weak Warnings, Zero Grammar Errors, Zero Language-Level Suggestions & Zero
   Regressions**:
    - All unit tests must pass before completing any task (`./gradlew test`).
    - After every single edit and prior to pushing code, inspect targeted files using IDE inspection tools
      (`get_file_problems` and `lint_files` via `execute_tool`).
    - All compilation errors (`ERROR`), compiler/inspection warnings (`WARNING`), code style/inspection hints
      (`WEAK WARNING`), spelling/grammar mistakes in comments/strings/docs, and language level change suggestions (Java
      25 modernization, redundant casts/types, sequenced collection replacements) must be fixed immediately.
    - The repository baseline must remain at zero compiler warnings, zero test failures, zero inspection warnings, and
      zero weak warnings.
7. **Inspect Before Modifying**: Read targeted production and test files before making code edits. Never propose changes
   based on unverified assumptions.
8. **`.gitignore` and Dot-Folders Are NOT `.aiignore`**: Standard search tools (`grep_search`, `find_by_name`)
   automatically ignore hidden dot-folders and patterns in `.gitignore`. AI agents MUST NOT treat these files as
   excluded or nonexistent. All files under [
   `.ai/decisions/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/), [
   `.ai/context/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/), [
   `.ai/prompts/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/), `docs/`, and reference codebases
   (`compact/`, `intellij-elixir/`, `intellij-rust/`, `intellij-scala/`, `Rplugin/`) are vital project assets that must
   be directly accessed via `client_view_file` or `view_file`.
9. **Ground Truth & Anti-Hardcoding Rigor**: Every architectural decision and implementation must cite upstream ground
   truth (`compact/compiler/parser.ss`, `lexer.ss`, `langs.ss`, `midnight-ledger.ss`) and cross-verify with production
   reference plugins (`intellij-rust`, `intellij-scala`, `intellij-elixir`, `Rplugin`). Never accept brittle hardcoded
   heuristics when scalable grammar-driven models exist.
10. **Multi-Tier Testing Standard**:
    - **Tier 1 (Golden Tree Conformance)**: Validate full PSI tree node-by-node against golden `.txt` trees without
      errors.
    - **Tier 2 (Partial Parsing & Error Recovery)**: Test with deliberately broken syntax (missing semicolons, unclosed
      braces) ensuring `PsiErrorElement` is registered without freezing EDT.
    - **Tier 3 (Operator Precedence & Associativity Matrix)**: Test complex binary, bitwise, boolean, relational, and
      range expression trees to guarantee correct AST hierarchy.
    - **Tier 4 (Stress & Stack Resilience)**: Test resilience against deeply nested parentheses, brackets, and blocks
      (preventing `StackOverflowError`).
    - **Tier 5 (Non-Blocking Concurrency)**: All inspections, linters, and index operations must yield immediately upon
      cancellation without UI thread lag.
11. **Mandatory Task Lifecycle & Completion Gate**: Every change must adhere to the lifecycle in [
    `.ai/workflow.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/workflow.md), including documentation
    decision rules, anti-assumption re-read verification, and the final completion gate.
12. **Persistent Bug Knowledge Base**: Whenever a concrete defect or incorrect behavior is identified, investigated,
    fixed, and verified, the agent MUST automatically document it as a markdown record in [
    `.ai/bugs/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/bugs/) using the standard bug record schema and
    register it in [`.ai/bugs/README.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/bugs/README.md).
    Before investigating non-trivial bugs, agents must search `.ai/bugs/` for prior occurrences. A task involving a
    resolved bug is NEVER complete until the bug record is written and indexed.
13. **Mandatory Git Worktree Isolation**: Every development session that modifies repository files MUST operate in a
    dedicated, isolated Git worktree (`../midnight-plugin-wt-<task-slug>` or `.worktrees/<task-slug>`) on a dedicated
    branch (`ai/<task-slug>`). NEVER work directly on `master`. All implementation, testing, documentation, `.ai/`
    updates, and commits must take place inside the worktree. Upon task completion and verification, changes are
    committed, merged into `master`, `master` is verified, and the worktree and branch are deleted. Purely read-only
    query tasks that do not modify files are exempt. See [
    `.ai/workflow.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/workflow.md) for the complete operational
    lifecycle.
14. **Dual-Tier Documentation & User-Facing Release Changelog Hygiene**:
    - **For AI & Developers (Engineering Depth)**: Deep architectural context, AST node mechanics, class/method names,
      upstream compiler lines, and test counts belong exclusively in [
      `.ai/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/), git commits, and code comments.
    - **For Plugin Users ([`CHANGELOG.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/CHANGELOG.md))**: Written
      strictly for IntelliJ plugin users, answering *"What changed for me?"*. It must remain clean, minimal,
      human-centric, and categorized under standard Keep-a-Changelog sections. Strip all internal class names, AST
      details, ADR numbers, and AI workflow references.
    - NEVER create or push a release tag while `CHANGELOG.md` still contains internal engineering notes.
15. **Continuous Inspection & Fix Loop**: Before doing work, during edits, after edits, and before pushing: whenever an
    issue is flagged or code is touched, fix it immediately. Never batch or postpone fixes.

---

## 4. Modern Java (Java 25) Guidelines & Feature Utilization

The plugin builds against Java 25 (`JavaLanguageVersion.of(25)`). Developers and AI agents **MUST utilize the full
spectrum of modern Java features introduced up to and including Java 25** (Java 14 through Java 25). Do NOT default to
obsolete pre-Java 17/21 idioms or hardcode legacy patterns:

1. **Java Records (`record`) & Immutability (Java 14+)**:
    - Use `record` for all immutable data carriers, DTOs, type descriptors, AST node pairs, resolver lookup keys,
      multi-value return containers, and cache keys.
    - **STRICTLY FORBIDDEN**: Writing boilerplate POJO classes with explicit private final fields, manual getters,
      `equals()`, `hashCode()`, and `toString()`.
    - Use compact constructors (`public MyRecord { ... }`) for component validation, normalization, and defensive
      copies.

2. **Sequenced Collections & Modern Indexing (Java 21+)**:
    - **STRICTLY FORBIDDEN**: Using `list.get(0)` or `list.get(list.size() - 1)` or manual iteration to fetch endpoints.
    - **ALWAYS USE**:
        - `list.getFirst()` / `deque.getFirst()` instead of `list.get(0)`.
        - `list.getLast()` / `deque.getLast()` instead of `list.get(list.size() - 1)`.
        - `list.addFirst(...)`, `list.addLast(...)`, `list.removeFirst()`, `list.removeLast()`.
        - `collection.reversed()` instead of manual index loops or `Collections.reverse()`.

3. **Pattern Matching for `instanceof` & `switch` (Java 16/21+)**:
    - **STRICTLY FORBIDDEN**: Using raw `if (x instanceof Foo) { Foo f = (Foo) x; }` casts.
    - Use pattern matching `if (x instanceof Foo f) { ... }`.
    - Use pattern matching switch expressions for PSI node dispatch, token classification, and type hierarchies:
      ```text
      return switch (element) {
          case CompactCircuitDefinition c -> handleCircuit(c);
          case CompactWitnessDefinition w -> handleWitness(w);
          case CompactLedgerDeclaration l -> handleLedger(l);
          case null, default -> null;
      };
      ```

4. **Record Patterns & Deconstruction (Java 21+)**:
    - Deconstruct records directly in `instanceof` and `switch` patterns:
      ```text
      if (entry instanceof ScopeBinding(var name, var psiElement, var namespace)) {
          // use deconstructed components directly
      }
      ```

5. **Unnamed Patterns and Variables (`_`) (Java 22+)**:
    - For unused exception variables, lambda arguments, or unneeded pattern components, use `_`:
      ```text
      try {
          // ...
      } catch (NumberFormatException _) {
          return null;
      }
      ```
    - **STRICTLY FORBIDDEN**: Declaring named variables that remain unused and trigger compiler/IDE warnings or weak
      warnings.

6. **Enhanced Switch Expressions with Arrow Syntax (Java 14+)**:
    - Always prefer exhaustive `switch` expressions returning values via `->` over fall-through statement switches with
      `break`.

7. **Sealed Classes & Interfaces (`sealed`, `permits`) (Java 17+)**:
    - Model closed AST element categories, semantic type systems, and compiler toolchain states using
      `sealed interface` / `sealed class` hierarchies to enable compile-time exhaustiveness checking.

8. **Multi-line Text Blocks (`"""..."""`) (Java 15+)**:
    - **STRICTLY FORBIDDEN**: Manual string concatenation with `\n` and `+` for multi-line strings.
    - Use multi-line text blocks for code snippets, HTML/XML inspection descriptions, test contract fixtures, and
      SQL/data templates.

9. **Modern Stream Gatherers & Stream API Improvements (Java 22+)**:
    - Utilize modern stream factory methods (`Stream.ofNullable()`, `toList()`) instead of verbose
      `Collectors.toList()`.

10. **Modern Immutable Collection Factories (Java 9+)**:
    - **STRICTLY FORBIDDEN**: Instantiating empty collections and immediately chaining `.add(...)` or wrapping with
      `Collections.unmodifiableList(...)`.
    - Always use `List.of(...)`, `Set.of(...)`, and `Map.of(...)` / `Map.ofEntries(...)`.

---

## 5. Mandatory Post-Edit & Pre-Push Code Inspection Protocol

> [!IMPORTANT]
> **CONTINUOUS INSPECTION AFTER EVERY EDIT & BEFORE EVERY PUSH**:
> Code quality in this repository is enforced through immediate, continuous static analysis. AI agents and developers
**MUST NOT batch inspections at the end of a session**. After **EVERY SINGLE EDIT** to any file (`.java`, `.xml`,
`.properties`, `.gradle.kts`) and prior to pushing any code or tags, the agent MUST run the inspection tools and resolve
all reported issues before proceeding.

### 5.1 Inspection Execution Order

Immediately following any file creation, modification, or prior to pushing code:

1. **Check Live Errors & Problems**:
   ```bash
   # Via idea execute_tool
   execute_tool --command "get_file_problems --filePath <absolute_path>"
   ```
   Inspect the `errors` array. Any item with severity `ERROR`, `WARNING`, or `WEAK WARNING` must be addressed.

2. **Run Static Inspections & Linter**:
   ```bash
   # Via idea execute_tool (quote JSON array properly)
   execute_tool --command "lint_files --files [\"<absolute_path>\"]"
   ```
   Inspect all returned inspection warnings, weak warnings, deprecation notices, grammar/spelling hints, and
   language-level change suggestions.

3. **Verify Build & Tests**:
   ```bash
   ./gradlew test
   ```
   Ensure 100% test pass rate with zero failures and zero compiler warnings.

### 5.2 Strict Zero-Tolerance Standard: Errors, Warnings, Weak Warnings & Suggestions

- **Errors (`ERROR`)**: Mandatory zero. Fix compilation failures, unresolvable symbols, type mismatches, and syntax
  errors immediately.
- **Warnings (`WARNING`)**: Mandatory zero. Fix deprecated API usages, unchecked operations, unhandled exceptions, raw
  types, and unused declarations.
- **Weak Warnings (`WEAK WARNING`)**: Mandatory zero. Fix code style hints, redundant qualifiers, suboptimal method
  calls (e.g., replacing `list.get(0)` with `list.getFirst()`), unneeded suppressions, and simplifyable conditionals.
- **Grammar & Spelling Errors**: Mandatory zero. Resolve all typos and grammatical issues in comments, JavaDocs,
  user-facing error messages, quick-fix descriptions, and intention names.
- **Language-Level Change Suggestions**: Mandatory upgrade. Whenever inspection tools suggest adopting modern Java 25
  idioms (such as replacing manual casts with pattern matching, adopting sequenced collection methods, simplifying
  lambdas, removing redundant type parameters `<>`, or converting concatenation to text blocks), apply the suggestions
  immediately.
- **Bugs & Regressions**: Must be caught immediately at the edit boundary and fixed before touching another file.

### 5.3 The Continuous Inspection-Fix-Verify Loop

```text
File Edited / Code Prepared for Push
   │
   ▼
Run get_file_problems & lint_files
   │
   ├─► Problems / Warnings / Weak Warnings / Suggestions Found?
   │     │
   │     ▼
   │   Fix issues immediately in target file
   │     │
   │     └─► Re-run get_file_problems & lint_files (Loop until 100% clean)
   │
   ▼
Zero Errors, Zero Warnings, Zero Weak Warnings, Zero Suggestions Confirmed
   │
   ▼
Proceed to Next Edit / Run Tests / Proceed to Push
```

- **Before doing work**: Verify that the starting files are clean.
- **During edits**: Check and fix after every single edit.
- **After edits / before push**: Re-inspect all modified files across the workspace.
- Never mark a task complete, create a commit, or push code while any file problem, warning, weak warning, grammar
  error, or language-level change suggestion remains unresolved.

---

## 6. Threading & Concurrency Rules

| Operation | Thread / Context | Mechanism | Existing Repo Example |
|:---|:---|:---|:---|
| **PSI Read** | Background Thread or EDT with **ReadAction** | Active in inspections, annotator `collectInformation` & `apply`; manual via `ReadAction.compute()` | [`CompactResolveUtil.resolve()`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/resolve/CompactResolveUtil.java) |
| **PSI Mutation** | **EDT only** with **WriteAction** & Command | `WriteCommandAction.runWriteCommandAction(project, () -> ...)` | [`CompactRemoveUnusedVariableFix.applyFix()`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/inspection/fix/CompactRemoveUnusedVariableFix.java) |
| **External Process Execution** | **Background Thread only** | `Task.Backgroundable`, `ExternalAnnotator.doAnnotate()`, `CommandLineState.startProcess()` | [`CompactExternalAnnotator.doAnnotate()`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/annotator/CompactExternalAnnotator.java) |
| **VFS Refresh** | Any thread (**asynchronous** strongly preferred) | `VfsUtil.markDirtyAndRefresh(true, ...)` | Output directory refresh |
| **UI Updates / Dialogs** | **EDT only** | `ApplicationManager.getApplication().invokeLater(...)` | [`CompactCreateFileAction.buildDialog()`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/actions/CompactCreateFileAction.java) |
| **Service Access** | Any thread | `@Service` + `getInstance()` | [`CompactStdlibService`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/stdlib/CompactStdlibService.java) |

- **RULE 1**: **NEVER run `process.waitFor()` or external CLI operations on the EDT.** It freezes the entire IDE.
- **RULE 2**: **NEVER modify PSI on a background thread.** All AST deletions, replacements, and additions must be
  executed inside a `WriteCommandAction` on the EDT.
- **RULE 3**: **Always support cancellation.** In long background loops, call `ProgressManager.checkCanceled()`
  periodically.

---

## 7. Tool Selection Priorities: IntelliJ MCP vs. CLI

For Java codebase exploration, refactoring, and code verification within this project, agents **MUST prioritize the
`idea` MCP server tools over raw CLI commands (`grep`, `find`, `cat`)**:

| Task | Preferred MCP Tool (`idea` server via `call_mcp_tool`) | Replaces CLI Tool | Reason |
|:---|:---|:---|:---|
| **Post-Edit Error & Problem Check** | `execute_tool --command "get_file_problems --filePath <path>"` | `./gradlew compileJava` | Instant (0s) check against IntelliJ's live error highlighter. Mandatory after EVERY edit. |
| **Post-Edit Static Code Inspection** | `execute_tool --command "lint_files --files [\"<path>\"]"` | CLI checkstyle | Evaluates project inspections across target files for warnings and weak warnings. |
| **Find Java Class / Method / Symbol** | `execute_tool --command "search_symbol --q <Name>"` | `grep_search`, `find_by_name` | Queries IntelliJ's live index; returns exact start/end line without false matches or noise (~40 tokens). |
| **Inspect Symbol Signature / JavaDoc** | `execute_tool --command "get_symbol_info --filePath <path> --line <L> --column <C>"` | `client_view_file`, `cat` | Returns exact signature, inheritance (`extends`, `implements`), and JavaDoc without dumping hundreds of file lines. |
| **Cross-File Renaming** | `execute_tool --command "rename_refactoring --pathInProject <p> --symbolName <old> --newName <new>"` | Manual search & replace | True AST refactoring updating declarations, calls, imports, and overrides safely. |
| **Code Formatting** | `execute_tool --command "reformat_file --files [\"<path>\"]"` | Manual formatting | Applies IntelliJ's exact code style settings. |
| **Discovered Run Configurations** | `execute_tool --command "get_run_configurations"` / `execute_run_configuration --configurationName <name>` | CLI command guessing | Leverages configured IntelliJ run targets directly. |

*Note: Use CLI/native tools (`client_view_file`, `run_command`) for non-Java files (`.compact`, Markdown, YAML, Gradle
configs) or if the IDE MCP server is unavailable.*

---

## 8. Reference Repository Selection Matrix

> [!IMPORTANT]
> **MANDATORY RULE FOR COMPARISONS**: Whenever asked to compare with production repositories or reference
implementations, ALWAYS compare against the four local reference repositories located directly in the workspace root:
> - **`intellij-elixir/`**: Handwritten Lexer/Parser, `LexerBase`, PSI infrastructure, and element factory.
> - **`intellij-scala/`**: Keyword completion scoping (`ScalaKeywordCompletionContributor`), compile server daemon,
    REPL.
> - **`intellij-rust/`**: Multi-level completion provider (`RsKeywordCompletionContributor` with `declarationPattern` vs
    `newCodeStatementPattern`), run configurations, `ExternalAnnotator`.
> - **`Rplugin/`**: WSL interpreter/toolchain discovery (`RToolchain`), Windows path translation, project generators.
>
> Never guess, generalize, or rely on hypothetical assumptions when these local production repositories are directly
available to inspect.

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

## 9. Critical Pitfalls & Anti-Patterns to Avoid

1. **The Windows `compact.exe` Trap**: Windows has a native NTFS compression utility at
   `C:\Windows\System32\compact.exe`. Never execute `findExecutableInPath("compact")` on Windows without prioritizing
   WSL and filtering out Windows system directories. Use [
   `CompactToolchainUtil`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/run/CompactToolchainUtil.java).
2. **Velocity `${NAME}` Pollution**: In file templates, the `${NAME}` property must be the pure simple identifier (e.g.,
   `Token`), not a file path (`sub/Token`) or file name (`Token.compact`).
3. **Bypassing `CreateFileAction.MkDirs`**: When creating files from templates with paths, always use `MkDirs` to create
   intermediate directories; calling `dir.createFile()` with path slashes throws `IncorrectOperationException`.
4. **Missing `<internalFileTemplate>`**: In modern IntelliJ Platform, bundled file templates (`.ft`) must be explicitly
   declared in `plugin.xml` with `<internalFileTemplate name="..."/>` or IntelliJ's usage statistics collector throws
   assertion errors in tests.
5. **Merging Value & Type Namespaces**: Compact has distinct namespaces. Resolving `Point` in an expression must never
   resolve to `struct Point` (type), and resolving `Point` in a type signature must never resolve to `const Point`
   (variable).
6. **Swallowing Exceptions**: Never use empty `catch (Exception _) { return null; }` blocks that mask genuine
   configuration errors or permissions issues from the user.
7. **Memory Leaks via Static PSI**: Never store `PsiElement`, `PsiFile`, or `Project` instances in static fields,
   long-lived caches, or non-disposable listeners. Use `CachedValuesManager` or `Disposer`.

---

## 10. Code Review & Improvement Bar (Zero Improvement Hallucinations)

When the user asks open-ended, casual, or vague questions such as *"Does this file need improvements?"*, *"Can this be
improved?"*, or *"Review this file"*:

1. **Default Verdict is "No Changes Needed"**:
    - The default and expected outcome for tested, working code is: **"No improvements required; this code is solid and
      production-ready."**
    - Do NOT treat review questions as an obligation or challenge to invent diffs or find something to modify.

2. **Strict Invariant Benchmark**:
   Only propose an improvement if there is a concrete, verifiable failure against one of these four criteria:
    - **Threading & Concurrency (Section 6)**: e.g., PSI read off ReadAction, PSI mutation off EDT/WriteCommandAction,
      or blocking `process.waitFor()` on the EDT.
    - **Critical Invariants (Section 3)**: e.g., Merged value/type namespaces, missing null/`PsiErrorElement` guards, or
      memory leaks via static PSI references.
    - **Language / Compiler Semantics**: Direct deviation from official compiler rules (`compact/compiler/` or [
      `.ai/context/compact-semantics.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/compact-semantics.md)).
    - **Correctness / Regression**: A tangible bug, broken test, or resource leak.

3. **Strictly Prohibited Proposals (Anti-Bikeshedding)**:
    - **NO cosmetic rewrites**: Do not convert working loops to streams, reformat working code, or swap functional vs.
      imperative style.
    - **NO speculative abstractions**: Do not invent interfaces, factories, or builder patterns where direct
      implementations already work.
    - **NO subjective renames**: Do not suggest renaming local variables or methods that already follow repository
      conventions.
    - **NO rewriting working architecture**: The parser, lexer, resolver, annotator, and PSI wrappers are verified and
      production-ready.

4. **Output Format when Clean**:
   If none of the four criteria are violated, state concisely:
   > **"No improvements required."** Followed by a 1–2 bullet summary confirming compliance with the relevant invariants
   (e.g., threading model, null-safety, test coverage).

---

## 11. Context Navigation & Task Routing Map

All detailed context and workflow documents reside under [
`.ai/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/):

| Need | Document | Description |
|:---|:---|:---|
| **Directory Index** | [`.ai/README.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/README.md) | Entry point & overview of the `.ai/` system |
| **Task Lifecycle & Gate** | [`.ai/workflow.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/workflow.md) | Standard 11-step workflow, Git worktree isolation, release changelog cleanup, decision rules, & final gate |
| **Custom Push Prompt** | [`.ai/prompts/push-and-release.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/push-and-release.md) | Master AI prompt and checklist for inspection, changelog cleanup, tagging, and pushing |
| **Machine State** | [`.ai/project-state.yaml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/project-state.yaml) | Machine-readable feature phases, test counts, and test suites |
| **Session Handoff** | [`.ai/handoff.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/handoff.md) | Current active feature, implementation status, and next priorities |
| **Architecture Guide** | [`.ai/context/architecture.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/architecture.md) | Detailed subsystem design, threading model, and extension points |
| **Current State** | [`.ai/context/current-state.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/current-state.md) | Narrative implementation snapshot and test breakdown |
| **Compact Semantics** | [`.ai/context/compact-semantics.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/compact-semantics.md) | Verified Compact language behavior, typing rules, and grammar |
| **IntelliJ Patterns** | [`.ai/context/intellij-patterns.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/intellij-patterns.md) | Plugin-specific IntelliJ Platform patterns, APIs, and comparative analysis |
| **Reference Map** | [`.ai/context/reference-map.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/reference-map.md) | Index of compiler (`compact/`) and reference plugins (`intellij-*/`, `Rplugin/`) |
| **Decisions (ADRs)** | [`.ai/decisions/README.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/README.md) | Master index of Architectural Decision Records (ADR-001 through ADR-028) |
| **Bug Knowledge Base** | [`.ai/bugs/README.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/bugs/README.md) | Searchable index of resolved debugging incidents, root causes, solutions, and prevention lessons |
| **User Changelog** | [`CHANGELOG.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/CHANGELOG.md) | Curated user-facing release notes and working Unreleased collection |

### Task-Specific Context Loading Router

When starting a task, consult the **Context Loading Matrix** in [
`.ai/workflow.md#3-phase-1-context-loading--task-routing`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/workflow.md)
to load only the required files and avoid token bloat.

---

## 12. Release, Changelog Hygiene, Semantic Tagging & Code Push Protocol

This protocol defines how releases are prepared, how the changelog is updated, how semantic version tags are calculated,
and how code and tags are pushed. For ready-to-run AI prompts and checklists, consult [
`.ai/prompts/push-and-release.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/push-and-release.md).

### 12.1 The Two Audiences: User-Facing Changelog vs. AI & Developer Knowledge

The repository maintains a strict separation between user release documentation and internal engineering records:

1. **For the AI and Developers (Internal Engineering Knowledge)**:
    - All deep technical implementation details, AST node names, class and method signatures, compiler line references
      (`compact/compiler/parser.ss`), test suites and counts, and internal AI documentation restructuring belong
      **strictly** in:
        - [`.ai/bugs/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/bugs/) (bug investigation records)
        - [`.ai/decisions/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/) (architectural decision
          records)
        - [`.ai/context/`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/) (architecture and current
          state)
        - Git commit messages (descriptive conventional commits)
        - Source code comments and JavaDoc annotations.
    - This information preserves the exact reasoning, history, and mechanics for future AI sessions and plugin
      maintainers.

2. **For Plugin Users ([`CHANGELOG.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/CHANGELOG.md))**:
    - Written strictly from the perspective of an IntelliJ IDEA user developing Compact contracts.
    - Answers: **"What changed for me?"**
    - Must be clean, minimal, concise, and understandable without access to internal source code.
    - Categorized strictly under standard Keep-a-Changelog sections (`### Added`, `### Changed`, `### Deprecated`,
      `### Removed`, `### Fixed`).
    - **Zero Technical Noise**: Never include Java class names (e.g. `CompactCompletionContributor`), method names
      (`isExportPreceding()`), internal AST structures, ADR identifiers, test method names or metrics, file paths, or
      internal AI restructuring notes.
    - Multiple related technical commits must be consolidated into 1–3 clear, high-level user-facing bullet points.

### 12.2 Semantic Versioning Tag Decision Matrix

Release tags follow Semantic Versioning (`vMAJOR.MINOR.PATCH`) based on the type of changes:

| Change Category | Trigger Conditions | SemVer Bump | Tag Example |
|:---|:---|:---|:---|
| **MAJOR** | Breaking syntax or grammar changes in Compact language support; removal of deprecated features; major incompatible platform changes. | `(X+1).0.0` | `v2.0.0` |
| **MINOR** | New user-facing features; new declarations or live templates; new intentions or semantic inspections; new tool windows (Remix compiler, status bar widget); new run configuration capabilities. | `X.(Y+1).0` | `v1.3.0` |
| **PATCH** | Bug fixes; false-positive or false-negative inspection corrections; editor freeze/crash prevention; performance optimizations; diagnostic translation fixes; non-breaking completion refinements. | `X.Y.(Z+1)` | `v1.2.7` |

### 12.3 Mandatory Pre-Push Inspection & Continuous Fix Loop

Before pushing any code or creating any release tag:

1. **Inspect All Target Files**: Run `execute_tool --command "get_file_problems --filePath <path>"` and `lint_files`.
2. **Eliminate All Diagnostics**:
    - 0 errors (`ERROR`)
    - 0 warnings (`WARNING`)
    - 0 weak warnings (`WEAK WARNING`)
    - 0 grammar and spelling errors
    - 0 unapplied language-level change suggestions (e.g. adopting modern Java 25 idioms like sequenced collections
      `getFirst()`/`getLast()`, pattern matching, record deconstruction, unnamed patterns `_`, text blocks, and modern
      immutable collections).
3. **Continuous Fix Loop**: If any issue is found, fix it immediately, re-inspect, and re-run tests until 100% clean.

### 12.4 Tag Creation and Push Sequence

Once inspections pass and `CHANGELOG.md` is verified:

1. **Update Version in `gradle.properties`**: Set `version=X.Y.Z`.
2. **Update Fallback Release Notes in `src/main/resources/META-INF/plugin.xml`**: Sync `<version>` and `<change-notes>`.
3. **Clean and Version `CHANGELOG.md`**: Move user-facing entries from `## [Unreleased]` into `## [X.Y.Z] - YYYY-MM-DD`
   and open a fresh empty `## [Unreleased]` block.
4. **Verify Test Suite and Build**:
   ```bash
   ./gradlew test -Pversion=X.Y.Z
   ./gradlew buildPlugin -Pversion=X.Y.Z
   ```
5. **Commit Release Changes**:
   ```bash
   git commit -m "chore(release): prepare vX.Y.Z"
   ```
6. **Create Annotated Tag**:
   ```bash
   git tag -a vX.Y.Z -m "Release vX.Y.Z: <clean user-facing summary>"
   ```
7. **Push Code and Tag to Remote**:
   ```bash
   git push origin master
   git push origin vX.Y.Z
   ```

---

## 13. System Governance, MCP Security & Protected Artifacts Guard

### 13.1 Path-Scoped Subsystem Rules
In addition to the global invariants in this document, agents MUST consult and adhere to path-scoped rule contracts located in [`.agents/rules/`](.agents/rules/):
- **PSI & Parser**: [`.agents/rules/psi-parser.rules.md`](.agents/rules/psi-parser.rules.md) (token advancement, incomplete code tolerance, namespace separation)
- **Threading & Memory**: [`.agents/rules/threading.rules.md`](.agents/rules/threading.rules.md) (ReadAction, WriteCommandAction, static PSI prohibition)
- **Modern Java 25**: [`.agents/rules/modern-java25.rules.md`](.agents/rules/modern-java25.rules.md) (records, sequenced collections, pattern matching, unnamed `_`)
- **Inspections & Annotators**: [`.agents/rules/inspections-annotators.rules.md`](.agents/rules/inspections-annotators.rules.md) (SideEffectGuard in previews, 3-phase annotator)
- **Toolchain & WSL**: [`.agents/rules/toolchain-wsl.rules.md`](.agents/rules/toolchain-wsl.rules.md) (Windows `compact.exe` filter, WSL path translation)

### 13.2 Living Operational & Architectural Knowledge Subsystems
- **Bug Escalation & Recurring Patterns**: [`.ai/bugs/recurring-patterns.md`](.ai/bugs/recurring-patterns.md) (Tracks chronic anti-patterns across incidents)
- **Test Inventory & Execution Tiers**: [`.ai/testing/test-inventory.md`](.ai/testing/test-inventory.md)
- **Flakiness Register & Quarantine Log**: [`.ai/testing/flakiness-log.md`](.ai/testing/flakiness-log.md)
- **Security Boundaries & Threat Model**: [`.ai/security/threat-boundaries.md`](.ai/security/threat-boundaries.md)
- **Banned Patterns & Vulnerability Rules**: [`.ai/security/vulnerability-rules.md`](.ai/security/vulnerability-rules.md)
- **Deployment & Operational Runbooks**: [`.ai/operations/deployment-runbooks.md`](.ai/operations/deployment-runbooks.md)
- **Architecture Drift Ledger**: [`.ai/meta/drift-audit-ledger.md`](.ai/meta/drift-audit-ledger.md)
- **Platform Deprecation Register**: [`.ai/meta/deprecation-register.md`](.ai/meta/deprecation-register.md)

### 13.3 Automated Multi-Gate Verification Runner
Local and pre-commit verification can be run via the PowerShell automation script:
```powershell
# Fast compilation and plugin structure check
powershell -ExecutionPolicy Bypass -File .\scripts\verify-patch.ps1 -Quick

# Targeted test verification
powershell -ExecutionPolicy Bypass -File .\scripts\verify-patch.ps1 -TestPattern dev.verloren.midnight.CompactBundleTest
```
Outputs machine-readable JSON status at `build/verification-report.json`.

### 13.4 Tool Execution Gateways & Model Routing
- **MCP Tool Manifest & Scopes**: [`.agents/config/mcp-gateways.json`](.agents/config/mcp-gateways.json)
- **Asymmetric Local Model Routing**: [`.agents/config/model-routing.yaml`](.agents/config/model-routing.yaml)

### 13.5 Immutable Protected Artifacts Guard
The following files and contracts are **PROTECTED ARTIFACTS**. AI agents are **STRICTLY PROHIBITED** from automatically modifying, deleting, or weakening them without explicit user consent:
1. **Core Architectural Invariants** in [`AGENTS.md`](AGENTS.md) Section 3 and Section 6.
2. **Accepted Architectural Decision Records** in [`.ai/decisions/`](.ai/decisions/) (`ADR-001` through `ADR-034`). ADRs can be superseded by new ADRs, never edited retrospectively.
3. **Security Boundaries & Vulnerability Policies** in [`.ai/security/`](.ai/security/).
4. **Tool Gateway Manifest** in [`.agents/config/mcp-gateways.json`](.agents/config/mcp-gateways.json).
5. **Existing Unit / Integration Tests**: AI agents must never delete or `@Disabled`-annotate failing tests to artificially pass verification gates.
