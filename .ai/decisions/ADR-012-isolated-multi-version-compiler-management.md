# ADR-012: Isolated Multi-Version Compiler Management & SemVer Normalization

## Status
Accepted (Phase 23–24 / Complete)

## Context & Problem Statement
Smart contract ecosystems evolve rapidly, and different Midnight contracts require different compiler versions (e.g. `pragma language_version >= 0.23;` vs `pragma language_version >= 0.26;`).
A single global compiler binary on the host PATH fails when:
1. Opening legacy projects requiring an older compiler alongside modern projects requiring a newer compiler.
2. Two contracts within the same workspace declare different pragma constraints.
3. Developers must manually search GitHub releases, download `.tar.gz` archives, unpack binaries, and configure PATH variables.
4. Pragma versions frequently omit patch digits (e.g. `0.23` instead of `0.23.0`), breaking strict 3-part SemVer parsers.

## Authoritative References
1. **Official Midnight Toolchain Releases**:
   - GitHub Releases: `midnightntwrk/compact` (`0.34.0` -> Compact language `0.26.0`; `0.31.1` -> Compact language `0.23.0`).
2. **Compact Pragma Specifications**:
   - [`compact/compiler/parser.ss:138-145`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/compact/compiler/parser.ss#L138-L145): `pragma language_version` comparison and validation.
3. **Production JetBrains Implementations**:
   - `intellij-rust`: Toolchain channels (stable, nightly) and SDK version selection.
   - `intellij-scala`: SBT/Scala version multi-SDK management.

## Decision
1. **Isolated Version Directory Layout**:
   - Maintain installed compiler versions in an isolated sandbox under `~/.compact/versions/<version>/` (or inside WSL under Linux user home on Windows).
   - Each directory contains the self-contained executable binary `compact` or `compactc`.
2. **Automated Asynchronous Download & Extraction**:
   - Provide a background downloader (`CompactVersionManager.downloadAndInstallVersion`) using Java 25 `HttpClient` and `Task.Backgroundable` with IntelliJ progress dialogs.
   - Automatically unpack `.tar.gz` / `.zip` platform archives and assign executable permissions (`setExecutable(true)`).
3. **Two-Digit Loose SemVer Normalization (`CompactSemVerUtil`)**:
   - Normalizes two-part versions (`0.23` -> `0.23.0`) and bare numbers (`0` -> `0.0.0`).
   - Supports comparison expressions: `^`, `~`, `>=`, `<=`, `==`, `>`, `<`.
   - Maps compiler toolchain versions to language versions (`0.34.0` -> `0.26.0`, `0.31.1` -> `0.23.0`).
4. **Per-Project Version Persistence**:
   - Persist active project compiler version in `.idea/midnight.xml` via `MidnightProjectSettings` (`PersistentStateComponent`).
   - Resolution chain: File pragma -> Project setting -> Global IDE settings -> Host PATH.

## Scalability & Anti-Hardcoding Evaluation
- **Is it hardcoded?**: No. The SemVer comparison engine parses arbitrary semantic version strings dynamically. New releases can be added or detected on GitHub without breaking existing version parsing.
- **Is it isolated?**: Yes. Installing or switching a compiler version never modifies system PATH or interferes with other projects.

## Feature Implementation Map
- Version Manager: [`CompactVersionManager.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/version/CompactVersionManager.java)
- SemVer Utility: [`CompactSemVerUtil.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/version/CompactSemVerUtil.java)
- Project Settings: [`MidnightProjectSettings.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/settings/MidnightProjectSettings.java)
- Global Settings: [`MidnightSettingsState.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/settings/MidnightSettingsState.java)
- Unit Tests:
  - [`CompactVersionManagerTest.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/version/CompactVersionManagerTest.java)
  - [`CompactSemVerUtilTest.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/version/CompactSemVerUtilTest.java)

## Consequences & Future Maintenance
- When new major compiler releases appear upstream, add their version mappings to `KNOWN_VERSIONS` and update bundled language version mappings.
