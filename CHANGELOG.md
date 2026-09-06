<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Midnight-plugin Changelog

## [Unreleased]
### Added
- **Marketplace Plugin Recommendations (`dependencySupport`)**:
  - Registered `dependencySupport` extension points in `plugin.xml` for JavaScript/TypeScript projects referencing `@midnight-ntwrk/compact-runtime`, `@midnight-ntwrk/compact-js`, `@midnight-ntwrk/midnight-js-contracts`, and `@openzeppelin/compact-contracts`.
  - Enables IntelliJ Platform IDEs (IntelliJ IDEA, WebStorm) to automatically recommend the Midnight Compact plugin when opening projects with Midnight and Compact dependencies.
- **Project Structure & Module Scaffolding Roadmap**:
  - Outlined `CompactModuleType`, `CompactModuleBuilder`, and `DirectoryProjectGenerator` architecture in `docs/future_features_and_blockchain_ide_roadmap.md` for dedicated "New Project" wizard scaffolding and JetBrains Feature Extractor module-type recommendation scanning.

## [1.2.1] - 2026-09-06
### Fixed
- **DaemonCodeAnalyzer Deprecated API & IDE Stability**:
  - Replaced deprecated `DaemonCodeAnalyzer.restart()` and `restart(PsiFile)` calls with diagnostic reason-aware APIs (`restart(reason)` and `restart(psi, reason)`).
  - Fixed cascading analysis restart storms: updating a single pragma/file now selectively restarts highlighting only for the target file instead of rescheduling the daemon across all open and project files.

## [1.2.0] - 2026-09-06
### Added
- **Compact Toolchain vs. Language Version Alignment**:
  - Distinguishes Compact Compiler Toolchain releases from Compact Language specification versions (`0.34.0` implements Language `0.26.0`, `0.31.1`/`0.31.0` implements Language `0.23.0`, `0.30.0` implements Language `0.22.0`, and `0.26.0` implements Language `0.18.0`).
  - Version cards in the tool window explicitly display both toolchain and language versions: `Compact v<toolchain> (Language v<langVer>)`.
  - In-editor pragma validation and quick-fixes accurately map `pragma language_version >= 0.26.0;` to toolchain `0.34.0`.
- **Enhanced Compiler Path Discovery & Sync**:
  - Dynamically auto-detects installed compilers under `~/.compact/versions/*/compactc` (both on host OS and inside WSL).
  - Automatically synchronizes `MidnightSettingsState.compilerPath` upon selecting any compiler version card, updating editor annotations and run configurations immediately without requiring an IDE restart.\n  - Corrected official GitHub release download URLs to use `compactc-v<version>` tag patterns with automatic cleanup on download failure.
- **Native In-Process Pragma Inspection (`CompactPragmaVersionInspection`)**:
  - Registered a native `LocalInspectionTool` (level `ERROR`) that directly evaluates contract `pragma language_version` constraints against the active toolchain.
  - Immediately updates inline red error squiggly underlines and the editor's top-right traffic light upon compiler changes or file edits without waiting for background external processes.
- **Instant Error & Traffic Light Synchronization**:
  - Switching or deleting compiler versions immediately triggers daemon code analysis, providing instantaneous feedback in the editor and Problems tool window.
- **Interactive Run Button with Green Outline**:
  - Redesigned the \"Run Contract\" button in the Compact Compiler tool window with an emerald green outline, execution icon, and responsive hover/press styling.\n  - Automatically resolves the target `.compact` file from the focused editor, open tabs, or indexed project files, executing compilation and streaming live output to the Run console.
- **Uniform Version Cards & Action Controls**:
  - Standardized all version cards to a fixed height (`JBUI.scale(42)`) with clean single-action icons (Download for uninstalled, Delete for installed).
  - Added a download confirmation dialog to prevent accidental triggers.
- **Windows / WSL Detection & Binary Architecture**:
  - Automatically detects Windows environments and directs Compact compiler operations into WSL (`/home/<user>/.compact/versions/<version>/`).
  - Auto-migrates legacy Windows compiler versions into WSL and automatically sets Linux executable permissions (`chmod -R +x`).
  - Supports native execution on Linux and macOS hosts.
- **Contract Implementation Gutter Markers**:
  - Added navigation gutter icons for `contract ... implements <Interface>` declarations to jump directly to implemented interfaces.

### Fixed
- **Synchronous Execution on EDT / ReadAction Assertion (`OSProcessHandler.checkEdtAndReadAction`)**:
  - Introduced memory-cached version registry (`EXECUTABLE_VERSION_CACHE`), path-based SemVer extraction, and asynchronous background scheduling for external binaries, strictly preventing EDT/ReadAction freezes.
- **WSL IJENT Communication Failures (`IjentUnavailableException$CommunicationFailure`)**:
  - Replaced speculative distro probing with native `WslDistributionManager.getInstance().getInstalledDistributions()`.
- **Missing Error Indicator on Deleted Compiler**:
  - Fixed problem indicator clearing when deleting the active compiler; native pragma inspection flags missing or incompatible compilers immediately.
- **WSL Binary Execution for Configured Version**:
  - Ensured WSL binary paths (`/home/...`) set `isWsl = true` on Windows, preventing native process spawning failures.
- Wrapped PSI queries in safe `ReadAction` blocks in `CompactCompilerPanel` to prevent threading assertions.
- Fixed `NullPointerException` from `NotificationGroupManager.getNotificationGroup(\"Compact Compiler\")`.
- Migrated `CompactSpellcheckingStrategy` to text-level spellchecking for IntelliJ 2024.2+ platform compatibility.
- Fixed false-positive recursive circuit warnings in `CompactRecursiveCircuitInspection` when wrapper circuits call external imported circuits sharing the same base name.

## [1.1.0] - 2026-03-24
### Fixed
- Updated CI/CD pipeline with marketplace token and gradlew executable permissions.

## [1.0.0] - 2026-03-20
### Added
- Initial release of Midnight Compact IntelliJ plugin.
- Syntax highlighting, lexer/parser, code completion, and external annotator support for Compact contracts.
