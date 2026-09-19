# Toolchain WSL Path Translation Hardening and Codebase Inspection Cleanup

- **Date:** 2026-09-19
- **Feature / Component:** toolchain / psi / inspection-cleanup
- **Severity:** Medium
- **Status:** Resolved

## Symptoms

1. `CompactToolchainUtil.toWindowsPath()` triggered inspection warnings due to redundant `path.isEmpty()` checks and lacked boundary protection when converting short or non-standard WSL `/mnt/` mount paths.
2. In `CompactToolchainUtil.findInWsl()`, an auxiliary `ArrayList` was allocated and sorted instead of sorting arrays directly.
3. Redundant dead methods (`getProgramElements()`, `getImportElements()`, `getBodyElements()`, `hasToken()`) and unused parameters existed across `CompactFile`, `CompactImportDeclarationImpl`, `CompactModuleDefinitionImpl`, and `CompactResolveUtil`.
4. Several test fixtures contained legacy string concatenations instead of Java 25 text blocks, and `DeclarationPsiTest` lacked unit test coverage for `CompactModuleDefinition.getMembers()`.

## Context

During project-wide code quality inspection and proofreading under the zero-tolerance inspection policy (0 errors, 0 warnings, 0 weak warnings), multiple compiler warnings, weak warnings, and path conversion edge cases were detected and addressed across production and test suites.

## Root Cause

1. **WSL Path Parsing Boundary**: `toWindowsPath()` checked `path.startsWith("/mnt/")` and parsed the character at index 5 without verifying that the string was at least 6 characters long or followed by a `/` or end-of-string.
2. **Dead Code & Legacy Annotations**: Historical evolution of AST accessors left unused private helpers and legacy `org.jspecify.annotations` imports instead of JetBrains `@NotNull`/`@Nullable` annotations.
3. **Test Inconsistencies**: Multi-line test files used `+` string concatenation, and status bar widget tests passed `null` to `@NotNull` parameters.

## Investigation

Inspected all 208 Java files using IntelliJ MCP `lint_files` and `get_file_problems`. Traced all reported inspection items:
- Checked `CompactToolchainUtil` against `.agents/rules/toolchain-wsl.rules.md`.
- Traced PSI hierarchy in `CompactModuleDefinitionImpl`, `CompactImportDeclarationImpl`, and `CompactFile`.
- Verified resolve helpers in `CompactResolveUtil` to ensure removing unused `PsiElement place` parameters did not alter multi-file or stdlib scoping behavior.

## Solution

1. **WSL Path Translation**:
   - Hardened `CompactToolchainUtil.toWindowsPath()` to guard `path.length() >= 6` and check trailing delimiters before extracting drive letters.
   - Replaced temporary collection allocations in `findInWsl()` with in-place array sorting (`Arrays.sort(verDirs, ...)`).
2. **PSI & Resolve Cleanup**:
   - Removed dead accessors (`getProgramElements()`, `getImportElements()`, `getBodyElements()`, `hasToken()`).
   - Cleaned up unused `place` parameters in `CompactResolveUtil` private helper methods.
   - Replaced `org.jspecify.annotations` with JetBrains annotations.
3. **Test Modernization**:
   - Converted test fixtures in `CompactDocCommentEnterTest`, `CompactSmartEnterTest`, and `CompactPhase28IntentionsTest` to Java 25 text blocks (`"""..."""`).
   - Added dedicated unit test `DeclarationPsiTest.testModulePsiAccessors()` for `CompactModuleDefinition.getMembers()`.
   - Hardened `CompactStatusBarWidgetTest` with `TestActionEvent` and `WindowManager` status bar resolution.

## Verification

1. **Automated Verification Harness**:
   - Executed `powershell -ExecutionPolicy Bypass -File .\scripts\verify-patch.ps1 -AllTests`.
   - All 700 automated unit and integration tests passed (`overall_status: "PASSED"`).
2. **MCP Code Inspection**:
   - Executed `lint_files` across all modified files; returned 0 errors, 0 warnings, 0 weak warnings (`{"items": []}`).

## Prevention / Lesson

- Always ensure string index lookups in path translation utilities (`/mnt/<drive>/...`) are guarded by strict length and delimiter bounds checks.
- When adding new AST node classes or resolve helpers, ensure all helper parameters and accessors are actively consumed and verified with dedicated test coverage.

## Related Files

- `src/main/java/dev/verloren/midnight/run/CompactToolchainUtil.java`
- `src/test/java/dev/verloren/midnight/run/CompactToolchainUtilTest.java`
- `src/main/java/dev/verloren/midnight/psi/CompactFile.java`
- `src/main/java/dev/verloren/midnight/psi/CompactImportDeclarationImpl.java`
- `src/main/java/dev/verloren/midnight/psi/CompactModuleDefinitionImpl.java`
- `src/main/java/dev/verloren/midnight/resolve/CompactResolveUtil.java`
- `src/test/java/dev/verloren/midnight/psi/DeclarationPsiTest.java`
- `src/test/java/dev/verloren/midnight/statusbar/CompactStatusBarWidgetTest.java`

## Related ADRs / Context

- `.agents/rules/toolchain-wsl.rules.md`
- `.agents/rules/modern-java25.rules.md`
- `.agents/rules/psi-parser.rules.md`
