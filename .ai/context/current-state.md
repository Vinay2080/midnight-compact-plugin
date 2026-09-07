# Current State

Last Updated: September 2026 (v1.2.0)

---

## 1. Implementation Snapshot

### Implemented Subsystems
- **Lexer & Parser**: Handwritten in Java 25. Complete coverage of Compact grammar, declarations, ledger types, type expressions, statements, expressions, and error recovery.
- **PSI Infrastructure**: Element hierarchy (`CompactElement`, `CompactNamedElement`, declaration types, reference types, type nodes).
- **Name Resolution & Reference Contributor**: Lexical scoping, namespace separation (`VALUE` vs `TYPE`), multi-file resolution via `include` statements.
- **Code Completion**: Keyword completion, built-in type completion, scope-aware local variable and parameter completion, circuit and ledger field completion.
- **Semantic Inspections**:
  - Unresolved references (`CompactUnresolvedReferenceInspection`)
  - Duplicate declarations (`CompactDuplicateDeclarationInspection`)
  - Unused local variables (`CompactUnusedLocalVariableInspection`) with quick-fix
  - Type mismatch (`CompactTypeMismatchInspection`)
  - Pure circuit violation (`CompactPureCircuitInspection`)
  - Sealed ledger mutation (`CompactSealedFieldMutationInspection`)
  - Recursive circuit detection (`CompactRecursiveCircuitInspection`)
  - Constructor restrictions (`CompactConstructorRestrictionInspection`)
  - Undisclosed witness usage (`CompactUndisclosedWitnessInspection`)
  - Pragma compiler version mismatch (`CompactPragmaVersionInspection`)
- **Code Style & Formatting**: Spacing rules, smart indentation, brace matching, quote handling, and commenter.
- **IDE Features**: Structure view, syntax color settings page, breadcrumbs, code folding, hover documentation provider, live templates, and file templates.
- **Run Configurations & Toolchain Integration**: Compiler run configuration, gutter run line markers, toolchain discovery for Linux, macOS, Windows native, and WSL.
- **Line Markers**: Bidirectional navigation between interface declarations, circuits, and contract implementations.
- **Compiler Version Management**:
  - Isolated multi-version directory structure under `~/.compact/versions/<version>/`.
  - Automated binary download and installation with progress indicators and platform archive unpacking.
  - Per-project compiler version persistence in `.idea/midnight.xml` (`MidnightProjectSettings`).
  - Active version resolution chain: per-project setting -> global settings -> system PATH / WSL.
- **Remix-Style Tool Window**:
  - Right-hand stripe tool window ("Compact Compiler") with SVG branding icon.
  - Interactive compiler version switcher dropdown.
  - Real-time pragma version compatibility indicator with color-coded status badges.
  - "Download More..." compiler version management dialog.
  - "Compile Current Contract" action triggering background compilation and problem reporting.
- **Pragma Quick-Fixes & Context Actions**:
  - `Alt + Enter` context action to switch project compiler version to match contract pragma.
  - `Alt + Enter` context action to download a missing required compiler version.
  - `Alt + Enter` context action to update pragma version in file to match active compiler.
  - Quick-fixes attached directly to compiler version mismatch warnings.
- **Phase 27: Status Bar Toolchain & Environment Widget**:
  - `CompactStatusBarWidgetFactory`: Registered in `plugin.xml` on editor status bar.
  - `CompactStatusBarWidget`: Lightweight, non-blocking widget displaying active Compact version with language version mapping.
  - `CompactStatusBarPopup`: Native speed-search popup menu to switch installed compiler versions, download new versions, jump to the Remix Compiler panel, or open Midnight settings.
- **Declarative Inlay Hints**: Parameter name hints for circuits, witnesses, and constructors.
- **External Annotator**: Asynchronous background compiler diagnostic pipeline with cancellation support.
- **Code Modernization (Java 25 & IntelliJ 2026.2+)**:
  - Replaced deprecated `PathEnvironmentVariableUtil.findInPath` with modern Java `ProcessBuilder` and `where.exe`/`which` resolution.
  - Migrated `DaemonCodeAnalyzer.restart()` to `restart(reason)` and `restart(psiFile, reason)` with selective restart triggers.
  - Upgraded code constructs across 11 files:
    - Pattern matching for `switch` and `instanceof`.
    - Local variable type inference (`var`).
    - Sequenced collections (`SequencedMap`, `firstEntry()`, `lastEntry()`, `getFirst()`, `getLast()`).
    - Handled null selectors in pattern switch expressions (`case null, default ->`) to prevent JVM `NullPointerException`s.
    - Resolved constructor `[this-escape]` warnings with `@SuppressWarnings("this-escape")`.
  - Executed `./gradlew test` with 441 passing unit tests across 52 test classes.

### Roadmap & Evolution (Phases 27–34)
- **Phase 27: Status Bar Toolchain & Environment Monitor** (`CompactStatusBarWidgetFactory`) - **COMPLETE**.
- **Phase 28: New Project & DApp Scaffolding Wizard** (`CompactNewProjectWizard`, `DirectoryProjectGenerator`).
- **Phase 29: Persistent Stub Indexing Engine** (`CompactNamedElementIndex`, `CompactCircuitIndex`).
- **Phase 30: Modern Symbol API & Advanced Refactoring Processors** (`CompactRenameProcessor`, `CompactInlineConstantHandler`).
- **Phase 31: Blockchain IDE (Remix-Style) Explorer & Dynamic Circuit Invocation** (`MidnightExplorerToolWindowFactory`, `CompactDeployAndRunPanel`, witness drawer).
- **Phase 32: Ledger Storage Inspector & ZK Constraint Profiler** (TreeTable privacy visualizer, constraint metrics).
- **Phase 33: Embedded IntelliJ PSI-Powered MCP Server** (`CompactMcpServerService`, 11 semantic tools).
- **Phase 34: Polyglot TypeScript Bridge & Smart Contract Testing Framework** (`CompactTestFinder`, TS cross-navigation).

---

## 2. Test Verification Status

- **Total Unit Tests**: **441 passing** across 52 test classes (zero failures, zero skipped, 100% success rate).
- **Execution Command**: `./gradlew test`
- **Compiler Warnings**: **0 warnings** with `-Xlint:all`.
- **Architectural Documentation**: Full plan documented in `docs/FUTURE_PLAN.md`, `docs/XML_GAP_ANALYSIS.md`, `docs/REMIX_IDE_FEATURES.md`, `docs/MCP_SERVER_SPEC.md`.
