# IntelliJ Platform Patterns & Conventions

This guide documents established IntelliJ Platform APIs, concurrency rules, and architectural patterns in `midnight-plugin`, compared directly against official reference implementations (`intellij-rust`, `intellij-scala`, `intellij-elixir`, `Rplugin`).

---

## 1. Architectural Pattern Catalog

### Pattern 1.1: Handwritten Recursive-Descent Lexer & Parser
- **Pattern**: Handwritten recursive-descent lexer extending `LexerBase` and parser implementing `PsiParser` without Grammar-Kit/JFlex dependencies.
- **Why we use it**: Compact's grammar requires fine-grained error recovery (skipping unexpected tokens, recovering after semicolons, handling contextual keywords) and zero build-time code generation overhead.
- **Reference implementation**:
  - `intellij-elixir/src/org/elixir_lang/lexer/LexerBase.java`
  - `intellij-elixir/src/org/elixir_lang/parser/Parser.java`
- **Our implementation**:
  - [`CompactLexer.java`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/lexer/CompactLexer.java)
  - [`CompactParser.java`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/parser/CompactParser.java)
- **When to use**: Parsing Compact source files, adding new language expressions, statements, or top-level forms.
- **When NOT to use**: Do not replace with Grammar-Kit `.bnf` or external compiler binaries for IDE parsing. Parser loops must guarantee token advancement on every iteration.

---

### Pattern 1.2: Element Factory & Leaf Token Replacement
- **Pattern**: Programmatic generation of AST elements from text snippets via `PsiFileFactory`, replacing child nodes in-place.
- **Why we use it**: Renaming identifiers (`setName`), quick-fixes (`CompactRemoveUnusedVariableFix`), and AST refactorings must preserve surrounding whitespace, comments, and node hierarchy.
- **Reference implementation**:
  - `intellij-rust/src/main/kotlin/org/rust/lang/core/psi/RsPsiFactory.kt`
  - `intellij-elixir/src/org/elixir_lang/psi/ElementFactory.java`
- **Our implementation**:
  - [`CompactElementFactory.java`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/psi/CompactElementFactory.java)
  - [`CompactNamedElementImpl.setName()`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/psi/CompactNamedElementImpl.java)
- **When to use**: Symbol rename, AST snippet generation in intentions and quick fixes.
- **When NOT to use**: Never manipulate raw document strings or character ranges when modifying code structure; mutate the PSI tree inside a `WriteCommandAction`.

---

### Pattern 1.3: Single-File Scoping with Split Namespaces
- **Pattern**: Lexical scope walking with strict separation between value symbols (`VALUE`) and type symbols (`TYPE`), prioritizing innermost block bindings.
- **Why we use it**: Compact permits identical names across different namespaces (e.g. variable `Point` vs struct `Point`) and requires local parameter/const bindings to shadow outer or file-level definitions.
- **Reference implementation**:
  - `intellij-rust/src/main/kotlin/org/rust/lang/core/resolve/RsResolveProcessor.kt`
- **Our implementation**:
  - [`CompactResolveUtil.java`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/resolve/CompactResolveUtil.java)
- **When to use**: Resolving references (`resolveValue`, `resolveType`), populating completion variants, finding declarations.
- **When NOT to use**: Do not merge `VALUE` and `TYPE` lookups into a single untyped search; do not perform disk scans inside local scope walking.

---

### Pattern 1.4: Cross-File Reference with PSI Modification Caching
- **Pattern**: Resolving file paths in `include "path.compact";` to target `CompactFile` with results cached against `PsiModificationTracker.MODIFICATION_COUNT`.
- **Why we use it**: Repeatedly resolving file paths on disk during high-frequency editor typing passes causes severe UI lag. Caching invalidates automatically when any PSI file in the project changes.
- **Reference implementation**:
  - `intellij-scala/scala/scala-impl/src/org/jetbrains/plugins/scala/caches/BlockModificationTracker.scala`
- **Our implementation**:
  - [`CompactIncludeDeclarationImpl.resolveIncludedFile()`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/psi/CompactIncludeDeclarationImpl.java)
  - [`CompactIncludeReference.java`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/reference/CompactIncludeReference.java)
- **When to use**: Any cross-file or path-based resolution where target files are derived from string literals.
- **When NOT to use**: Do not cache raw mutable objects without `CachedValueProvider.Result`; do not cache objects whose lifecycle exceeds the target project.

---

### Pattern 1.5: PolyVariant Reference with ResolveCache & Recursion Guard
- **Pattern**: Subclassing `PsiPolyVariantReferenceBase` and delegating resolution to IntelliJ's platform `ResolveCache` with `needToPreventRecursion = true`.
- **Why we use it**: Prevents stack overflows on recursive definitions, circular type references, and redundant resolver invocations during IDE inspection runs.
- **Reference implementation**:
  - `intellij-rust/src/main/kotlin/org/rust/lang/core/resolve/ref/RsReferenceBase.kt`
- **Our implementation**:
  - [`CompactReferenceBase.java`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/reference/CompactReferenceBase.java)
- **When to use**: All identifier references in expressions, calls, types, and member accesses.
- **When NOT to use**: Do not implement raw `PsiReference` without caching unless resolution is provably instantaneous (e.g. trivial parent pointer).

---

### Pattern 1.6: Recursion-Guarded Type Evaluation
- **Pattern**: Protecting type inference evaluation loops with thread-local `RecursionGuard<PsiElement>` created via `RecursionManager.createGuard(...)`.
- **Why we use it**: Compact allows recursive expressions, cyclical type aliases, and pattern bindings. Without recursion guards, mutual type evaluation triggers `StackOverflowError`.
- **Reference implementation**:
  - `intellij-scala/scala/scala-impl/src/org/jetbrains/plugins/scala/lang/psi/types/`
- **Our implementation**:
  - [`CompactPatternImpl.getType()`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/psi/CompactPatternImpl.java)
  - [`CompactConstBindingImpl.getType()`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/psi/CompactConstBindingImpl.java)
- **When to use**: Type inference methods that inspect initializer expressions, aliases, or recursive struct definitions.
- **When NOT to use**: Do not use non-thread-safe `UserData` flags (e.g. `putUserData(RESOLVING_TYPE, true)`) which cause race conditions in concurrent editor analysis.

---

### Pattern 1.7: File Creation Action with MkDirs & Input Validation
- **Pattern**: Extending `CreateFileFromTemplateAction`, handling nested directory paths via `CreateFileAction.MkDirs`, sanitizing Velocity `${NAME}` variables, enforcing identifier validation via `InputValidatorEx`, and persisting default template selections.
- **Why we use it**: Users frequently type nested paths (`contracts/tokens/Token`) in the New File dialog. Standard platform handlers create intermediate folders, sanitize identifier variables, and trigger editor opening and code style formatting.
- **Reference implementation**:
  - `intellij-rust/src/main/kotlin/org/rust/ide/actions/RsCreateFileAction.kt`
  - `Rplugin/src/org/jetbrains/r/actions/NewRScriptAction.kt`
- **Our implementation**:
  - [`CompactCreateFileAction.java`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/actions/CompactCreateFileAction.java)
- **When to use**: Implementing file, contract, or module creation dialogs from templates.
- **When NOT to use**: Do not call `FileTemplateUtil.createFromTemplate` directly without `MkDirs`; do not swallow exceptions with silent fallbacks.

---

### Pattern 1.8: Toolchain Discovery & Cross-Platform WSL Path Translation
- **Pattern**: Multi-tier toolchain discovery (Settings -> Project `node_modules` -> WSL -> System PATH -> Package managers) with automatic Windows-to-WSL `/mnt/c/` path rewriting.
- **Why we use it**: Windows hosts Midnight developers who run `compact` inside Ubuntu WSL distributions. Furthermore, Windows contains a built-in `C:\Windows\System32\compact.exe` (NTFS compression utility) that MUST be explicitly filtered out.
- **Reference implementation**:
  - `Rplugin/psi/src/com/intellij/r/psi/interpreter/RInterpreterUtil.kt`
  - `intellij-rust/src/main/kotlin/org/rust/cargo/toolchain/wsl/RsWslToolchain.kt`
- **Our implementation**:
  - [`CompactToolchainUtil.java`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/run/CompactToolchainUtil.java)
- **When to use**: Invoking external CLI compilers (`compact`, `compactc`) from Run Configurations or background linters.
- **When NOT to use**: Never execute CLI binaries directly on the Event Dispatch Thread (EDT).

---

### Pattern 1.9: Context-Aware Run Configuration Producer
- **Pattern**: Subclassing `RunConfigurationProducer<CompactRunConfiguration>`, extracting file/circuit context from `ConfigurationContext`, and configuring per-contract deterministic output directories (`gen/<contract-path>`).
- **Why we use it**: Enables 1-click execution from the editor gutter, context menu, and "Current File" top toolbar run widget while preventing artifact collisions between multiple contracts.
- **Reference implementation**:
  - `intellij-rust/src/main/kotlin/org/rust/cargo/runconfig/producer/CargoRunConfigurationProducer.kt`
- **Our implementation**:
  - [`CompactRunConfigurationProducer.java`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/run/CompactRunConfigurationProducer.java)
  - [`CompactRunLineMarkerContributor.java`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/run/CompactRunLineMarkerContributor.java)
- **When to use**: Enabling gutter play buttons and context-aware execution of Compact contracts.

---

### Pattern 1.10: Asynchronous External Annotator with Timeout
- **Pattern**: Implementing `ExternalAnnotator<InitialInfo, AnnotationResult>` in 3 distinct phases: `collectInformation` (ReadAction), `doAnnotate` (Background thread with process execution), and `apply` (ReadAction).
- **Why we use it**: The official `compactc` compiler produces authoritative type and circuit verification diagnostics that cannot be replicated cheaply in PSI. Running it via `ExternalAnnotator` ensures editor responsiveness without freezing the UI thread.
- **Reference implementation**:
  - `intellij-rust/src/main/kotlin/org/rust/ide/annotator/RsExternalLinterAnnotator.kt`
- **Our implementation**:
  - [`CompactExternalAnnotator.java`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/annotator/CompactExternalAnnotator.java)
  - [`CompactCompilerOutputParser.java`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/annotator/CompactCompilerOutputParser.java)
- **When to use**: Projecting authoritative diagnostics from external compiler processes into editor annotations.
- **When NOT to use**: Do not use for instant syntax errors; use lightweight parser error elements and local inspections for fast feedback.

---

### Pattern 1.11: Local Inspection with Quick-Fix via Write Command
- **Pattern**: Extending `LocalInspectionTool`, inspecting AST elements using `CompactVisitor`, and providing `LocalQuickFix` fixes executed inside IntelliJ write transactions.
- **Why we use it**: Instant, keystroke-by-keystroke feedback on language invariants (e.g. `pure circuit` mutations, sealed ledger writes, unhandled witnesses) with 1-click automated repairs (`Alt + Enter`).
- **Reference implementation**:
  - `intellij-rust/src/main/kotlin/org/rust/ide/inspections/`
- **Our implementation**:
  - [`CompactPureCircuitInspection.java`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/inspection/CompactPureCircuitInspection.java)
  - [`CompactRemoveUnusedVariableFix.java`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/inspection/fix/CompactRemoveUnusedVariableFix.java)
  - [`CompactWrapWithDiscloseFix.java`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/inspection/fix/CompactWrapWithDiscloseFix.java)
- **When to use**: Reporting semantic violations, unused bindings, or type warnings in open editors.
- **When NOT to use**: Do not run inspections on trees containing `PsiErrorElement` (guard with `PsiTreeUtil.hasErrorElements()`).

---

### Pattern 1.12: Thread-Safe Project Service for Bundled Stdlib
- **Pattern**: Project-level service (`@Service(Service.Level.PROJECT)`) managing bundled standard library virtual files (`LightVirtualFile`), registered as read-only library roots.
- **Why we use it**: Provides global resolution and `Ctrl + Click` navigation to `Maybe`, `Counter`, `Map`, and ZKIR primitives without requiring the user to copy compiler files into their workspace.
- **Reference implementation**:
  - `intellij-rust/src/main/kotlin/org/rust/cargo/project/model/impl/CargoProjectsServiceImpl.kt`
- **Our implementation**:
  - [`CompactStdlibService.java`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/stdlib/CompactStdlibService.java)
  - [`CompactStandardLibraryProvider.java`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/stdlib/CompactStandardLibraryProvider.java)
- **When to use**: Providing bundled language runtime or standard library files to project symbol search.

---

## 2. Future Roadmap Architectural Patterns

### Pattern 2.1: Project Template & DApp Wizard (Roadmap)
- **Pattern**: Implementing `DirectoryProjectGenerator` and `ProjectTemplatesFactory` to scaffold complete Midnight DApps (Compact contract, TypeScript front-end, test suite, and configuration).
- **Reference implementation**:
  - `Rplugin/src/org/jetbrains/r/projectGenerator/REmptyProjectGenerator.kt`
  - `intellij-scala/scala/scala-impl/src/org/jetbrains/plugins/scala/project/template/`

### Pattern 2.2: Interactive Compiler Daemon / REPL Console (Roadmap)
- **Pattern**: Long-running background compiler server managing warm compiler memory states, communicating over sockets/IPC with output folding.
- **Reference implementation**:
  - `intellij-scala/scala/compile-server/src/org/jetbrains/plugins/scala/server/CompileServerManager.scala`
  - `intellij-scala/scala/repl/src/org/jetbrains/plugins/scala/console/`
  - `Rplugin/src/org/jetbrains/r/console/RConsoleRunner.kt`
