# ADR-011: Toolchain Discovery, WSL Translation, & Windows compact.exe Evasion

## Status
Accepted (Phases 20–22 / Complete)

## Context & Problem Statement
The Compact compiler toolchain is distributed as native CLI binaries (`compact` or `compactc`) on Linux, macOS, and Windows. On Windows, many blockchain developers execute compilers inside WSL (Windows Subsystem for Linux) environments (Ubuntu, Debian).
Two critical pitfalls threaten Windows toolchain execution:
1. **The Windows `compact.exe` Trap**: Windows includes a native NTFS compression utility located at `C:\Windows\System32\compact.exe`. Any naive executable search in PATH (e.g. `which compact` or `PathEnvironmentVariableUtil.findInPath("compact")`) inadvertently picks up this Windows compression binary, causing catastrophic failures or hanging when invoked with compiler arguments.
2. **Path Mismatch in WSL**: Windows paths (e.g. `C:\Users\shaki\contracts\token.compact`) cannot be passed directly to a Linux binary running inside WSL; they must be mapped to `/mnt/c/Users/shaki/...` using IntelliJ's `WSLDistribution.getWslPath()`.

## Authoritative References
1. **Midnight Compiler Distribution Artifacts**:
   - Official release archives: `compact-linux-x86_64.tar.gz`, `compact-macos-aarch64.tar.gz`, `compact-windows-x86_64.zip`.
2. **Production JetBrains Implementations**:
   - `Rplugin`: [`RToolchain.kt`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/Rplugin/src/org/jetbrains/r/interpreter/RToolchain.kt) and WSL path translation wrappers.
   - `intellij-rust`: Cargo toolchain discovery and WSL execution delegates.

## Decision
1. **Multi-Stage Discovery Precedence**:
   Toolchain discovery resolves in this strict priority:
   - **Step 1: Isolated Project Version**: Check `CompactVersionManager` for the project-pinned compiler version in `~/.compact/versions/<version>/`.
   - **Step 2: Explicit User Setting**: Check IDE settings (`MidnightProjectSettings` / `MidnightSettingsState`).
   - **Step 3: WSL Toolchains (Windows)**: Query `WslDistributionManager.getInstance().getInstalledDistributions()` and search WSL distribution PATHs before scanning Windows host system paths.
   - **Step 4: Host PATH with Anti-NTFS Filter**: Search host PATH, but **strictly reject** any executable path containing `System32`, `SysWOW64`, or matching Windows NTFS compression tools.
2. **WSL Command Line Construction**:
   When a WSL toolchain is selected, construct the execution command via `WSLDistribution.createCommandLine()`. All input file paths and output directory parameters are translated to POSIX paths via `distro.getWslPath(windowsPath)`.
3. **Immutable `ToolchainInfo` Record**:
   Toolchain discovery returns a Java 25 `record ToolchainInfo(String executablePath, boolean isWsl, String wslDistribution, boolean isCompactCli)` providing transparent execution metadata.

## Scalability & Anti-Hardcoding Evaluation
- **Is it hardcoded?**: No. It relies on IntelliJ's native `WslDistributionManager` for dynamic distribution detection rather than hardcoded `wsl.exe -d Ubuntu` commands.
- **Does it protect against system corruption?**: Yes. Filtering out `System32/compact.exe` guarantees that the OS compression tool is never triggered.

## Feature Implementation Map
- Toolchain Utility: [`CompactToolchainUtil.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/run/CompactToolchainUtil.java)
- Settings State: [`MidnightSettingsState.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/settings/MidnightSettingsState.java), [`MidnightProjectSettings.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/settings/MidnightProjectSettings.java)
- Version Manager: [`CompactVersionManager.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/version/CompactVersionManager.java)
- Unit Tests: [`CompactToolchainUtilTest.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/run/CompactToolchainUtilTest.java)

## Consequences & Future Maintenance
- All new execution components (External Annotator, Run Configurations, Compiler Panel) must obtain their `GeneralCommandLine` via `CompactToolchainUtil` to inherit WSL and Windows safety automatically.
