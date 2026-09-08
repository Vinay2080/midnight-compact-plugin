<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Midnight-plugin Changelog

## [1.2.3] - 2026-09-09
### Added
- **Dynamic Real-Time Compact Compiler Tool Window Updates**:
  - Wired `CompactCompilerEventListener` across project message buses to broadcast toolchain switches, installation changes, and pragma updates immediately to the UI.
  - Connected `FileEditorManagerListener` and `DocumentListener` to `CompactCompilerPanel`, ensuring active contract cards, pragma constraints, compatibility badges (`✔ pragma match`), and download/run actions dynamically refresh as soon as any `.compact` file is opened, selected, or edited.
  - Connected `CompactStatusBarWidget` to `CompactCompilerEventListener` for instantaneous status bar version and compatibility updates.

### Fixed
- **SemVer Pragma Constraint Handling for Bare Versions (`0.23` vs `0.26`)**:
  - Fixed constraint evaluation in `CompactSemVerUtil`: bare versions without operators (e.g. `pragma language_version 0.23;`) are now interpreted as `>= 0.23` instead of strict equality `==`, allowing higher compliant compilers (such as language `0.26` / toolchain `0.34.0`) to satisfy contracts without false-positive compiler mismatch warnings.
  - Added support for compound clauses with logical OR (`||`) and logical AND in `CompactSemVerUtil`.
- **Two-Digit SemVer Normalization & Proper Toolchain Resolution**:
  - Enhanced `CompactVersionManager.cleanVersion` to normalize two-part versions (e.g. `0.23` -> `0.23.0` and `0.26` -> `0.26.0`).
  - Corrected language-to-toolchain mapping so `0.23` maps to official toolchain `0.31.1` (Language `0.23.0`, Ledger 8) and `0.26` maps to `0.34.0` (Language `0.26.0`, Ledger 9), eliminating inaccurate fallback downloads of legacy toolchain `0.23.0` (which only implemented Language `0.15.0`).
- **Pragma Underline & Quick-Fix Synchronization**:
  - Differentiated `pragma compiler_version` from `pragma language_version` in `CompactPragmaVersionInspection`, `CompactSwitchCompilerQuickFix`, and `CompactUpdatePragmaQuickFix`.
  - Ensured that updating pragma or switching compilers re-evaluates inspections and restarts code analysis cleanly without leaving stale error underlines under the `pragma` keyword.

## [1.2.2] - 2026-09-08
### Added
- **Status Bar Toolchain & Environment Monitor (Phase 27)**:
  - Registered `CompactStatusBarWidgetFactory` (`CompactStatusBarWidget`) in `plugin.xml` on the editor status bar (`order="after CodeStyleStatusBarWidget, before git, before Notifications"`).
  - Lightweight, non-blocking widget displaying active Compact compiler version along with its corresponding language version mapping (e.g. `Compact: v0.34.0 (0.26.0)`).
  - Native speed-search popup menu (`CompactStatusBarPopup`) providing:
    - Active compiler toolchain header.
    - 1-click installed version switcher with checkmark indicators.
    - Reset to auto-detected system/WSL toolchain.
    - Background compiler downloader with non-blocking progress dialog.
    - Quick link to open the right-stripe **Compact Compiler** (Remix-style) tool window.
    - Quick link to open Midnight plugin configuration settings (`ShowSettingsUtil`).
  - Strict $O(1)$ in-memory evaluation on UI threads: widget never invokes external processes or probes disk synchronously on the Event Dispatch Thread (EDT).
  - Comprehensive unit test suite `CompactStatusBarWidgetTest` covering widget lifecycle, presentation text, file enablement, and popup actions (raising total suite to 441 passing tests).
- **Go to Type Declaration (`Ctrl+Shift+B` / `Cmd+Shift+B`)**:
  - Implemented `CompactTypeDeclarationProvider` (`com.intellij.codeInsight.navigation.actions.TypeDeclarationProvider`) to navigate directly from variables, parameters, expressions, and ledger fields to the underlying struct, enum, or type declaration.
  - Recursion-safe unwrapping of generic parameters (e.g. `Cell<T>`, `Vector<T, N>`) and type aliases down to nominal declarations.
  - Seamless navigation across cross-file includes and module exports via `CompactResolveUtil`.
  - Zero interference or side effects on the code completion engine (`CompactCompletionContext`).
- **Parameter Info & Signature Help (Ctrl+P / Cmd+P)**:
  - Implemented CompactParameterInfoHandler for in-editor parameter hint tooltips during invocation of functions, circuits, and constructor declarations.
  - Highlights active parameter indices dynamically based on caret location within comma-separated argument lists.
  - Provides full parameter descriptions and types modeled via CompactParametersDescription.
  - Tolerant of trailing commas, whitespace, and incomplete syntax trees without throwing PSI exceptions.
- **Marketplace Plugin Recommendations (`dependencySupport`)**:
  - Registered `dependencySupport` extension points in `plugin.xml` for JavaScript/TypeScript projects referencing `@midnight-ntwrk/compact-runtime`, `@midnight-ntwrk/compact-js`, `@midnight-ntwrk/midnight-js-contracts`, and `@openzeppelin/compact-contracts`.
  - Enables IntelliJ Platform IDEs (IntelliJ IDEA, WebStorm) to automatically recommend the Midnight Compact plugin when opening projects with Midnight and Compact dependencies.

### Fixed
- **Compiler Version Switching Code Duplication & Toolchain Sync**:
  - Unified duplicate background task and executable resolution logic between CompactSwitchCompilerQuickFix and CompactSwitchCompilerVersionIntention into CompactVersionManager.ensureAndSwitchVersion.
  - Deduplicated executable discovery and WSL path caching across compiler installation pathways in CompactVersionManager.
- **Run Configuration Producer Stability**:
  - Extracted shared context inspection in CompactRunConfigurationProducer to unify setupConfigurationFromContext and isConfigurationFromContext.
- **Enum and Struct Member Completion Resolution**:
  - Deduplicated PSI reference resolution loops for dot-access member completion in CompactCompletionContributor.
