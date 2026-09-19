# Security & Threat Boundaries Specification

This document defines the security boundaries, attack vectors, and defensive controls for the **Midnight Compact Language Plugin**.

---

## 1. Threat Boundaries & External Interfaces

```text
[Untrusted User Files (*.compact)]
       │
       ▼
[PSI Parser / AST Builder] ──── (Risk: StackOverflow on deeply nested syntax)
       │
       ▼
[Toolchain Execution (WSL / CLI)] ── (Risk: Command injection, Windows binary collision)
       │
       ▼
[External Process: compact compiler]
```

### Boundary 1: Untrusted Input Files (`.compact`)
- **Threat:** Malicious or malformed Compact source files containing deeply nested brackets (`((((...))))`) or unclosed blocks designed to exhaust memory or trigger `StackOverflowError`.
- **Mitigation:**
  - Parser expression loops enforce recursion depth ceilings.
  - Parser loops advance lexer unconditionally on unexpected tokens.

### Boundary 2: Toolchain CLI Process Execution
- **Threat 1: Command Injection:** If file names or compiler flags contain unescaped shell metacharacters (`&`, `|`, `;`), a compromised file could execute arbitrary host commands.
  - **Mitigation:** Never pass command strings to `Runtime.getRuntime().exec(String)` or `cmd.exe /c`. Always use `com.intellij.execution.configurations.GeneralCommandLine` with tokenized, parameterized arguments.
- **Threat 2: Windows System Binary Collision:** Invoking `compact` on Windows can launch `C:\Windows\System32\compact.exe` (NTFS compression).
  - **Mitigation:** `CompactToolchainUtil` must explicitly filter out `%SystemRoot%` and prioritize verified WSL Linux binaries.

### Boundary 3: File Template & Project Scaffolding Path Traversal
- **Threat:** User inputs file names containing relative traversal sequences (`../../etc/passwd` or `..\..\AppData`).
- **Mitigation:**
  - Sanitize input strings; strip directory separators before passing to `CompactCreateFileAction`.
  - Use IntelliJ's `CreateFileAction.MkDirs` to guarantee paths remain scoped to the chosen project directory.

### Boundary 4: Plugin Signing & Publishing Secrets
- **Threat:** Leakage of `CERTIFICATE_CHAIN`, `PRIVATE_KEY`, or `PUBLISH_TOKEN` in build logs, exception traces, or test outputs.
- **Mitigation:**
  - Secrets are injected strictly via CI environment variables.
  - Secrets are never hardcoded, written to test configurations, or referenced in `.ai/` records.
