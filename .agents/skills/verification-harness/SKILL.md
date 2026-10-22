---
name: verification-harness
description: Guides the execution, interpretation, and troubleshooting of the automated verification harness and test gates.
---

# Verification Harness

## Purpose
Guide running and interpreting multi-gate automated verification for `midnight-compact-plugin` before proposing or finalizing any code change.

## Verification Workflow

### Strict Precedence: JetBrains MCP First
Before running terminal verification scripts, AI agents **MUST invoke JetBrains MCP tools first**:
1. **Live Diagnostics**: `execute_tool get_file_problems --filePath "<path>"` (0 errors, 0 warnings).
2. **Code Inspections**: `execute_tool lint_files --files ["<path>"]`.
3. **Code Style**: `execute_tool reformat_file --filePath "<path>"`.
4. **Targeted Test Execution**: `execute_tool execute_run_configuration --name "<TestConfigurationName>"`.

### Secondary / Final Multi-Gate Verification (CLI)
After JetBrains MCP checks pass cleanly, run the multi-gate PowerShell verification script for full CI, plugin structure, and architecture validation:
```powershell
.\scripts\verify-patch.ps1 -TestPattern "dev.verloren.midnight.<target_subsystem>.*"
```
Or for full repository verification:
```powershell
.\scripts\verify-patch.ps1
```

### The 5 Gates
1. **Gate 0: Git Isolation**:
   - Ensures work is never performed directly on `master`.
   - Confirms that the current branch is an isolated task branch (e.g. `ai/engineering-improvement`).
2. **Gate 1: Compilation**:
   - Compiles both main and test sources with Java 25 (`JavaLanguageVersion.of(25)`).
   - Validates zero compiler warnings treated as errors.
3. **Gate 1b: Static Analysis & Code Style**:
   - Runs `checkstyleMain` and `checkstyleTest`.
   - Validates file length (<= 400 lines), method length (<= 40 lines), and naming/formatting standards.
   - Any violation produces an error in `build/reports/checkstyle/main.html` or `test.html`.
4. **Gate 2: Plugin Verification**:
   - Runs `./gradlew verifyPlugin` against IntelliJ Platform SDK.
   - Validates `plugin.xml` descriptors, action declarations, and extension point bindings.
5. **Gate 3: Automated Testing & Coverage**:
   - Executes JUnit 5 and IntelliJ Platform test cases.
   - Confirms 100% test pass rate for all targeted or repository suites.

### Machine-Readable Verification Report
The harness generates `build/verification-report.json` containing:
- `timestamp`: Execution time
- `branch`: Active branch
- `gates`: Status and messages for Gate 0, Gate 1, Gate 1b, Gate 2, Gate 3
- `overallResult`: "PASSED" or "FAILED"
- `summary`: High-level summary of verification outcomes
