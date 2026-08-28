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
- **352 automated unit tests passing** (0 failures, 0 skipped, 100% success rate across forty test suites).

## Tests
- **352/352 tests passing** (0 failures, 0 skipped, 100% success rate across forty test suites).
- Verified via `./gradlew test`.

## Next Feature Options
1. **New Project & DApp Wizard (`CompactProjectTemplateFactory`)**: Full project creation wizard with boilerplate contract and TypeScript integration.
2. **Interactive Debugger Framework (`XDebugger` & Breakpoints)**: Compact breakpoint types and execution trace viewer.




## Relevant Context
- Architecture details: [architecture.md](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/architecture.md)
- Current state: [current-state.md](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/current-state.md)
- Compact semantics: [compact-semantics.md](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/compact-semantics.md)
- IntelliJ patterns: [intellij-patterns.md](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/intellij-patterns.md)
