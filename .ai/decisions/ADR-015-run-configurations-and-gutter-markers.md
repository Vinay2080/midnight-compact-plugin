# ADR-015: Run Configurations & Context-Aware Gutter Play Actions

## Status
Accepted (Phase 17, 21 / Complete)

## Context & Problem Statement
Developers need to build and test Compact smart contracts easily without opening a terminal, remembering compiler flags, or configuring paths.
In IntelliJ, executable constructs are typically launched via:
1. Gutter green play icons (`AllIcons.Actions.Execute`) next to contract headers.
2. Context menu (**Run 'Compile contract.compact'**).
3. Persistent Run/Debug configurations saved in `.idea/runConfigurations`.

Creating a run configuration for Compact contracts requires:
- Automatic derivation of output artifact directories (`contract/build/` or `<project>/gen/`).
- Toggle for `--skip-zk` (fast compilation skipping heavy zero-knowledge circuit synthesis during development).
- Cross-platform path translation when running under WSL on Windows.
- Console error log filter turning compiler errors into clickable source links.

## Authoritative References
1. **Midnight Compiler CLI Invocations**:
   - `compact compile [--skip-zk] [--output-dir <dir>] <file.compact>`.
2. **Production JetBrains Implementations**:
   - `intellij-rust`: [`RsRunConfigurationProducer.kt`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/intellij-rust/src/main/kotlin/org/rust/cargo/runconfig/producer/RsRunConfigurationProducer.kt) and `RsRunLineMarkerContributor.kt`.

## Decision
1. **Configuration Producer (`CompactRunConfigurationProducer`)**:
   - Inherits `LazyRunConfigurationProducer<CompactRunConfiguration>`.
   - Automatically detects when the caret or context is inside a `CompactFile` or `CompactContractDefinition`.
   - Pre-populates the configuration name (`Compile <ContractName>`), file path, and smart output directory.
2. **Interactive Gutter Play Markers (`CompactRunLineMarkerContributor`)**:
   - Placed directly on the `contract` keyword identifier in the gutter.
   - Provides a one-click dropdown offering **Run 'Compile...'**, **Edit Configuration...**, and **Debug...**.
3. **Execution State & Console Filters (`CompactRunProfileState`)**:
   - Executes via `CompactToolchainUtil.createCommandLine()`.
   - Attaches `CompactConsoleFilter` to parse stderr lines (e.g. `contract.compact:14:5: error: ...`) into interactive IntelliJ hyperlinked navigation spans jumping directly to the source file offset.
4. **VFS Post-Execution Refresh**:
   - On process termination, automatically triggers `VfsUtil.markDirtyAndRefresh(true, ...)` on the target output directory so compiled bytecode, ABI, and TypeScript bindings appear in the Project View immediately.

## Scalability & Anti-Hardcoding Evaluation
- **Is it hardcoded?**: No. Dynamic inspection finds contracts from AST tokens, and compiler paths resolve via `CompactToolchainUtil`.
- **Is it user-extensible?**: Yes. Custom compiler arguments can be supplied in the Run Configuration UI editor (`CompactSettingsEditor`).

## Feature Implementation Map
- Run Configuration: [`CompactRunConfiguration.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/run/CompactRunConfiguration.java)
- Configuration Type: [`CompactConfigurationType.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/run/CompactConfigurationType.java)
- Producer: [`CompactRunConfigurationProducer.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/run/CompactRunConfigurationProducer.java)
- Gutter Marker: [`CompactRunLineMarkerContributor.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/run/CompactRunLineMarkerContributor.java)
- Profile State: [`CompactRunProfileState.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/run/CompactRunProfileState.java)
- Settings Editor: [`CompactSettingsEditor.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/run/CompactSettingsEditor.java)
- Registration: [`plugin.xml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/resources/META-INF/plugin.xml) (`<configurationType>`, `<runConfigurationProducer>`, `<runLineMarkerContributor>`)
- Unit Tests: [`CompactRunConfigurationTest.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/run/CompactRunConfigurationTest.java)

## Consequences & Future Maintenance
- Run configurations are saved natively in `.idea/runConfigurations/` or shared via version control.
- Future deployment steps (e.g. `compact deploy --network testnet`) can be added as distinct configuration types extending the same profile state patterns.
