# Current Handoff

## Current Feature
Phases 15–18: Compiler Run Configurations, Midnight Settings, External Linter Annotator & Semantic Gutter Markers.

## Status
- **Phase 15: Run Configurations & Gutter Play Buttons**:
  - Implemented [`CompactConfigurationType`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/run/CompactConfigurationType.java), [`CompactRunConfiguration`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/run/CompactRunConfiguration.java), [`CompactRunConfigurationEditor`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/run/CompactRunConfigurationEditor.java), [`CompactRunProfileState`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/run/CompactRunProfileState.java), [`CompactConsoleFilter`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/run/CompactConsoleFilter.java), and [`CompactRunLineMarkerContributor`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/run/CompactRunLineMarkerContributor.java).
- **Phase 16: Midnight Settings Configurable**:
  - Implemented [`MidnightSettingsState`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/settings/MidnightSettingsState.java), [`MidnightSettingsComponent`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/settings/MidnightSettingsComponent.java), and [`MidnightSettingsConfigurable`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/settings/MidnightSettingsConfigurable.java) under `Languages & Frameworks -> Midnight Compact`.
- **Phase 17: External Linter Annotator**:
  - Implemented [`CompactExternalAnnotator`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/annotator/CompactExternalAnnotator.java), [`CompactCompilerDiagnostic`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/annotator/CompactCompilerDiagnostic.java), and [`CompactCompilerOutputParser`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/annotator/CompactCompilerOutputParser.java) for background `compactc` diagnostics.
- **Phase 18: Semantic Gutter Line Markers**:
  - Implemented [`CompactLineMarkerProvider`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/editor/CompactLineMarkerProvider.java) rendering gutter indicators for `witness`, `disclose`, `circuit`, and `ledger` nodes.
- **Reference Resolution Thread-Safety & Idempotency**:
  - Replaced non-thread-safe `putUserData(RESOLVING_TYPE, ...)` in [`CompactPatternImpl`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/psi/CompactPatternImpl.java) with thread-local `RecursionGuard`.
  - Added `RecursionGuard` to [`CompactConstBindingImpl`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/psi/CompactConstBindingImpl.java).
  - Enabled `needToPreventRecursion = true` in [`CompactReferenceBase`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/reference/CompactReferenceBase.java) for IntelliJ's `ResolveCache`.
  - Implemented `getType()` on `CompactLedgerDeclarationImpl`, `CompactWitnessDeclarationImpl`, and `CompactCircuitDefinitionImpl`.
  - Added import & type-alias unwrapping to [`CompactStructFieldReference`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/reference/CompactStructFieldReference.java).
- **Cross-Platform & WSL Compiler Discovery & Execution**:
  - Implemented comprehensive WSL (Windows Subsystem for Linux) and native host support in [`CompactToolchainUtil`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/run/CompactToolchainUtil.java).
  - Automatically discovers `compact` and `compactc` binaries in WSL distributions (e.g. `Ubuntu`, `/home/<user>/.local/bin/compact`, `/usr/local/bin`, etc.) via network shares and standard paths.
  - Transparently translates Windows paths (`C:\...`) to WSL paths (`/mnt/c/...`) and injects the `compile` subcommand when invoking the `compact` CLI tool.
  - Added an "Auto-Detect" button and WSL path helper in [`MidnightSettingsComponent`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/settings/MidnightSettingsComponent.java).
  - Updated [`CompactRunProfileState`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/run/CompactRunProfileState.java) and [`CompactExternalAnnotator`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/annotator/CompactExternalAnnotator.java) to use unified `CompactToolchainUtil.createCommandLine`.
- **Context-Aware Run Configuration Producer & Gutter Actions**:
  - Implemented and registered [`CompactRunConfigurationProducer`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/run/CompactRunConfigurationProducer.java) (`<runConfigurationProducer>`).
  - Automatically produces and selects run configurations from the current active file, contract, or circuit context.
  - Automatically sets default output directory (`gen`) from settings, populates file paths, and configures `--skip-zk` flags.
  - Enables the top toolbar "Current File" run widget, editor context menu "Run 'Compile <file>'", and the left gutter green play icons.
- **Fix Compact Symbol Resolver for Top-Level Declarations & Forward References**:
  - Enhanced [`CompactResolveUtil`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/resolve/CompactResolveUtil.java) so top-level ledger declarations (and other top-level symbols) are visible throughout the entire source file regardless of declaration order.
  - Implemented `isTopLevelFileDeclaration` and `isDirectModuleDeclaration` filters to separate file/module-level global symbols from local block bindings and callable/lambda parameters.
- **Fix Compact Run Configuration Output Directory Handling**:
  - Implemented deterministic per-contract output directory derivation in [`CompactToolchainUtil.deriveOutputDirectory`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/run/CompactToolchainUtil.java).
  - Calculates unique output paths per contract (e.g. `circuits/calculator.compact` -> `gen/calculator`, `circuits/counter.compact` -> `gen/counter`, `circuits/bboard.compact` -> `gen/bboard`).
  - Preserves nested source directory subpaths (e.g. `circuits/tokens/erc20.compact` -> `gen/tokens/erc20`, `src/v1/counter.compact` -> `gen/v1/counter`) to avoid cross-contract artifact collisions and overwriting.
  - Generates directory structures fully compatible with Compact compiler artifacts (`compiler/`, `contract/`, `keys/`, `zkir/`).
- **Fix Ctrl+B / Ctrl+Click Navigation & Direct Reference Resolution for Bundled Standard Library Symbols**:
  - Implemented [`CompactReferenceContributor`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/reference/CompactReferenceContributor.java) registered under `<psi.referenceContributor>` to provide direct `PsiReference`s on Compact identifier tokens (`LeafPsiElement`) in type, expression, call, and struct literal positions.
  - Implemented [`CompactGotoDeclarationHandler`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/navigation/CompactGotoDeclarationHandler.java) registered under `<gotoDeclarationHandler>` for first-class Ctrl+B and Ctrl+Click navigation directly to declarations.
  - Enhanced [`CompactStandardLibraryProvider`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/stdlib/CompactStandardLibraryProvider.java) to ensure bundled `standard-library.compact` and `zkir-v3-library.compact` virtual files are properly registered with the VFS, marked read-only, and fully navigable in editor tabs.
  - Verified resolution and navigation for `Maybe<Field>`, `some(42)`, and `secp256k1EcdsaVerify`, while strictly preserving lexical shadowing over standard library declarations.
- **Phase 19: Architectural Hardening & Concurrency Safety**:
  - Implemented [`CompactStdlibService`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/stdlib/CompactStdlibService.java) (`@Service(Service.Level.PROJECT)`) providing thread-safe, race-free bundled standard library & ZKIR initialization with deterministic `0L` timestamp and registered in `plugin.xml`.
  - Configured `CompactParserDefinition.getStringLiteralElements()` returning `CompactTokenSets.STRING_LITERALS` enabling native IntelliJ string literal language injection and quote handlers.
  - Removed dead `BOOLEAN_LITERALS` lookup map in [`CompactLexer`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/lexer/CompactLexer.java).
  - Added non-annotated leaf punctuation and whitespace fast-exit check in [`CompactHighlightingAnnotator`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/highlighter/CompactHighlightingAnnotator.java).
  - Added `CompactFile.getTopLevelDeclarations()` avoiding deep AST recursive tree walks in `CompactResolveUtil`.
  - Created [`CompactTestUtils`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/CompactTestUtils.java) DSL test helpers (`doCheckResolve` / `doCheckNoResolve`) with marker DSL.
- **Phase 20: Return-Type Verification, Compiler Exception Diagnostics & Completion Prioritization**:
  - Implemented return statement type-checking in [`CompactTypeMismatchInspection`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/inspection/CompactTypeMismatchInspection.java) verifying `return <expr>` against enclosing `circuit`, `witness`, or constructor return types using `CompactType.isAssignableTo()` and `CompactPsiUtil.getCallableReturnType()`.
  - Added multi-line compiler exception diagnostic parsing in [`CompactCompilerOutputParser`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/annotator/CompactCompilerOutputParser.java) to parse `Exception: <file> line <line> char <col>:\n<details>` emitted by `compactc`.
  - Prioritized `true` and `false` literals (`PrioritizedLookupElement.withPriority(..., 100.0)`) in [`CompactCompletionContributor`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/completion/CompactCompletionContributor.java) for Boolean return contexts.
  - Added test cases in `CompactInspectionTest`, `CompactExternalAnnotatorTest`, and `CompactCompletionTest`.
- **Phase 21: File Templates & CompactCreateFileAction Optimization**:
  - Rewrote [`CompactCreateFileAction`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/actions/CompactCreateFileAction.java) with `findInternalTemplate()` template customization fallback, non-empty `InputValidatorEx`, and public testability methods.
  - Re-authored all 4 file templates in `src/main/resources/fileTemplates/internal/` to align with official Midnight Compact specifications:
    - `Compact Contract.compact.ft`: Complete smart contract scaffold with `import CompactStandardLibrary;`, `export ledger counter: Counter;`, `constructor()`, and `export circuit increment(): []`.
    - `Compact Module.compact.ft`: Valid modular library with `struct Config` and `pure circuit isValid(): Boolean`, removing illegal constructor and invalid `ledger { ... }` block.
    - `Compact Interface.compact.ft`: Valid external contract type declaration `export contract ${NAME} { circuit ...: []; }`, replacing illegal `export contract implements`.
    - `Compact File.compact.ft`: Clean pragma with standard library import.
  - Expanded [`CompactFileTemplateTest`](file:///c:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/ide/fileTemplates/CompactFileTemplateTest.java) to 7 comprehensive tests evaluating Velocity properties and verifying zero PSI parse errors across all templates and end-to-end file creation.
- **Phase 22: Hardened File Creation Action (`CompactCreateFileAction`)**:
  - Implemented nested subdirectory creation via `CreateFileAction.MkDirs` (e.g. `contracts/tokens/MyContract`).
  - Extracted pure base identifiers (`extractSimpleName`) to prevent Velocity `${NAME}` pollution.
  - Contextual `InputValidatorEx` using `CompactNamesValidator` to reject invalid filesystem characters, path errors, and language keywords (`circuit`, `witness`, `ledger`, `contract`).
  - Integrated platform lifecycle (`createFileFromTemplate`) with automated editor opening, FUS logging, code reformatting via `CodeStyleManager`, and caret positioning after identifiers.
  - Persisted user's last chosen template via `getDefaultTemplateProperty()`.
  - Registered all four file templates under `<internalFileTemplate>` in `plugin.xml`.
  - Expanded `CompactFileTemplateTest` to 12 tests verifying nested directories, suffix stripping, name extraction, and validation logic.
- **376 automated unit tests passing** (0 failures, 0 skipped, 100% success rate across forty-four test suites).

## Tests
- **376/376 tests passing** (0 failures, 0 skipped, 100% success rate across forty-four test suites).
- Verified via `./gradlew test`.

## Reference Repositories Integrated
- **`intellij-scala/`**: Referenced for external build servers / compiler daemons (`scala/compile-server/`), interactive REPL console (`scala/repl/`), structure view, and advanced type systems.
- **`Rplugin/`**: Referenced for dynamic interpreter and WSL discovery (`psi/.../interpreter/`), script run configurations (`src/.../run/`), New Project Wizard (`src/.../projectGenerator/`), and interactive tool windows (`src/.../visualization/`).
- **`../midnight-local-dev/`**: Referenced for local Docker Compose stack (`standalone.yml`), pre-funded accounts (`accounts.json`), contract build scripts (`private-identity-wallet/contracts/compile.ps1`), and default endpoints (node `9944`, proof server `6300`).

## Next Feature Options
1. **New Project & DApp Wizard (`CompactProjectTemplateFactory` / `CompactProjectGenerator`)**: Full project creation wizard with boilerplate contract, tsconfig, and wallet scaffolding (referencing `Rplugin/.../projectGenerator/` and `intellij-scala/.../project/template/`).
2. **Interactive Console & REPL Runner**: Interactive Compact execution scratchpad and terminal evaluator (referencing `intellij-scala/scala/repl/` and `Rplugin/.../console/`).
3. **Midnight Explorer Tool Window**: Tool window displaying connected Devnet / Localnet node status, block height, and account balances (referencing `../midnight-local-dev/` and `Rplugin/.../visualization/`).
4. **Interactive Debugger Framework (`XDebugger` & Breakpoints)**: Compact breakpoint types and execution trace viewer (referencing `intellij-scala/scala/debugger/`).

## Relevant Context
- Architecture details: [architecture.md](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/architecture.md)
- Current state: [current-state.md](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/current-state.md)
- Reference map: [reference-map.md](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/reference-map.md)
- Compact semantics: [compact-semantics.md](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/compact-semantics.md)
- IntelliJ patterns: [intellij-patterns.md](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/intellij-patterns.md)
