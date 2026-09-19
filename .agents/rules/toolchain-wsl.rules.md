# Path Rules: Toolchain Discovery & WSL Execution

**Scope:** `src/main/java/dev/verloren/midnight/run/**`, `src/main/java/dev/verloren/midnight/toolwindow/**`, `src/main/java/dev/verloren/midnight/statusbar/**`

## Invariants & Guardrails

1. **The Windows `compact.exe` Collision Trap**:
   - Windows natively provides `C:\Windows\System32\compact.exe` (NTFS compression utility).
   - Calling standard PATH search for `compact` will mistakenly pick up the Windows OS binary instead of the Compact language compiler.
   - Always prioritize WSL distributions (`wsl.exe`) or explicit toolchain configurations, and explicitly filter out Windows system directories. Use `CompactToolchainUtil`.

2. **WSL Path Translation**:
   - Windows paths (`C:\...`) must be translated to WSL paths (`/mnt/c/...`) when passing arguments to WSL-hosted `compact` binaries.
   - Output paths emitted by `compact` inside WSL must be translated back to Windows VFS paths before opening editors or locating source lines.

3. **Process Sandboxing & Timeouts**:
   - Every process execution via `GeneralCommandLine` must specify an explicit timeout (maximum 30 seconds for compilation, 5 seconds for version checks).
   - Ensure stdout and stderr streams are consumed asynchronously to prevent process deadlocks.
