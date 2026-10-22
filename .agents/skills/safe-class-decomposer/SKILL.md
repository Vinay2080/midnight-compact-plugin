---
name: safe-class-decomposer
description: Standard operating procedure for decomposing oversized classes into SRP delegates while preserving public API and zero regressions.
---

# Safe Class Decomposer

## Purpose
Provides a strict, regression-free standard operating procedure for breaking down large god classes (>400 lines) into focused, modular, Single Responsibility Principle (SRP) delegates.

## Step-by-Step Decomposition Protocol

### Step 1: Interface & Dependency Surface Analysis
1. Inspect the target class's public methods, package-private helpers, inner records/classes, and static constants.
2. Search all references across `src/main/` and `src/test/` using JetBrains MCP (`search_symbol`, `get_symbol_info`) first rather than CLI grep.
3. Identify logical cohesive clusters:
   - Token & Pattern parsers
   - Dedicated completion providers
   - Renderers vs parsers
   - Insert handlers vs lookup item factories

### Step 2: Extract Leaf Providers / Helpers First
1. Create new focused classes in the appropriate package.
2. Ensure each new file remains strictly <= 400 lines and every method <= 40 lines.
3. Apply modern Java 25 idioms (`record`, pattern matching switch expressions, sequenced collections).
4. Verify no upward dependency violations (check imports against layer hierarchy).
5. Apply code formatting and verify diagnostics via JetBrains MCP (`reformat_file`, `get_file_problems`).

### Step 3: Refactor the Main Class into a Coordinator
1. Keep the main class in its original package and location.
2. Retain all public methods and inner records/classes as delegating wrappers to avoid breaking callers or external tests.
3. Replace complex inlined logic with single-line calls to the extracted delegates.
4. Verify the coordinator is reduced to <= 400 lines.
5. Format and inspect via JetBrains MCP (`reformat_file`, `get_file_problems`, `lint_files`).

### Step 4: Clean Up Baseline Suppressions
1. Open `config/checkstyle/checkstyle-suppressions.xml`.
2. Remove the decomposed class from `<suppress checks="FileLength" files="ClassName\.java"/>`.
3. Remove the decomposed class from `<suppress checks="MethodLength" files="ClassName\.java"/>`.

### Step 5: Verification & Self-Correction (MCP First)
1. Verify 0 errors and 0 warnings via JetBrains MCP (`get_file_problems`, `lint_files`).
2. Run tests via JetBrains MCP live run configuration first: `execute_tool execute_run_configuration --name "<TestConfig>"`.
3. Run secondary verification harness for full multi-gate check: `./scripts/verify-patch.ps1 -TestPattern "dev.verloren.midnight.<subsystem>.*"`.
4. Fix any Checkstyle formatting, import, or length violations.
5. Ensure all automated tests pass 100% without weakening any assertions.
