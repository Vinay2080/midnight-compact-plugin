# Bug Record: CompactConsoleFilter Windows Path Failure, Scheme Exception Support, and DumbAware Compliance

**Date**: 2026-10-31  
**Subsystem**: Run Configuration / Console Execution Filter (`dev.verloren.midnight.run`)  
**Affected Component**: `CompactConsoleFilter.java`  
**Severity**: High  
**Status**: Resolved  

---

## 1. Symptoms & Failure Behavior

When developers executed Compact smart contract compilation tasks via Run Configurations:
1. **Windows Absolute Path Failure**: On Windows systems, compiler diagnostics emitting absolute paths with drive letters (e.g. `C:\projects\contract.compact:10:4: error: ...` or `C:/projects/...`) failed to be converted into clickable hyperlinks in the execution console.
2. **Chez Scheme Compiler Exception Failure**: When the compiler backend crashed or emitted Scheme-level compiler exceptions (e.g. `Exception in compile: contracts/counter.compact line 14, char 7: syntax error`), the coordinate was not detected or hyperlinked.
3. **Indexing Inactivity (Dumb Mode)**: Because `CompactConsoleFilter` did not implement `DumbAware`, console links were delayed or disabled during background project re-indexing.
4. **Missing Optional Column Handling**: Diagnostics without explicit column offsets (e.g. `contracts/counter.compact:20: error: ...`) failed to match the strict `:(\d+):(\d+)` pattern.
5. **Zero Test Coverage**: There were no regression or unit tests covering `CompactConsoleFilter`.

---

## 2. Context

During contract development and compilation, the compiler CLI (`compactc`) streams build messages, warnings, and errors across `stdout` and `stderr`. `CompactRunProfileState` attaches `CompactConsoleFilter` to convert coordinates into `OpenFileHyperlinkInfo` instances. However, the implementation had an unaddressed `//todo not yet complete` flag and fragile regex matching.

---

## 3. Root Cause Analysis

1. **Windows Drive Prefix Rejection in Regex**:
   `CompactConsoleFilter.ERROR_PATTERN` was defined as:
   ```java
   Pattern.compile("([a-zA-Z0-9_./\\\\-]+?\\.compact):(\\d+):(\\d+)(.*)");
   ```
   The file path character class did not allow colons (`:`). When encountered with Windows drive prefixes (e.g., `C:\...`), the match aborted or truncated because `:` was treated as the line delimiter instead of part of the drive letter.
2. **Missing `DumbAware` Interface**:
   IntelliJ console execution infrastructure pauses or skips non-`DumbAware` console filters while indexing files. Because `CompactConsoleFilter` performs only file system lookups and no PSI index operations, it should implement `DumbAware`.
3. **Unnormalized File Separators**:
   Windows paths containing backslashes (`\`) were passed directly into `LocalFileSystem.getInstance().findFileByPath(...)` without normalization via `FileUtil.toSystemIndependentName(...)`.
4. **Single Format Assumption**:
   The filter only targeted GNU-style `:line:col` diagnostics, ignoring Chez Scheme compiler exceptions (`file.compact line X, char Y`).
5. **FileSystem Scope Limitation in Testing and Project Lookups**:
   Relying purely on `LocalFileSystem.getInstance().findFileByPath(...)` failed to resolve virtual files when running inside in-memory testing fixtures (`TempFileSystem`) or projects resolved via `ProjectUtil.guessProjectDir(project)`.

---

## 4. Investigation & Evaluated Approaches

- **Reference Architecture Grounding**:
  - `intellij-rust`: Inspected `RsConsoleFilter.kt` and `RegexpFileLinkFilter.kt`. `RegexpFileLinkFilter` implements `Filter, DumbAware`, normalizes paths via `FileUtil.toSystemIndependentName`, and uses `FILE_POSITION_RE` with optional column support and drive letter patterns `(?:\\p{Alpha}:)?`.
  - `dev.verloren.midnight.annotator.CompactCompilerOutputParser`: Inspected the existing battle-tested diagnostic parser, which already supported both GNU diagnostics and Chez Scheme exceptions (`DIAGNOSTIC_PATTERN` and `EXCEPTION_PATTERN`).
- **Resolution Strategy**:
  - Aligned `CompactConsoleFilter` patterns with `CompactCompilerOutputParser` to ensure consistent diagnostic parsing across editor annotations and console output.
  - Utilized `ProjectUtil.guessProjectDir(project)` to guarantee seamless file resolution in both production local disks and in-memory test fixtures.

---

## 5. Solution & Implementation

1. **Implemented `DumbAware`**:
   Updated `CompactConsoleFilter implements Filter, DumbAware`.
2. **Dual-Pattern Regex Matching**:
   Added `DIAGNOSTIC_PATTERN` and `SCHEME_EXCEPTION_PATTERN`:
   - `DIAGNOSTIC_PATTERN`: `(?:^|(?<=[\\s\\[]))([a-zA-Z]:[\\\\/][^:\\r\\n]+?\\.compact|[^:\\r\\n\\s][^:\\r\\n]*?\\.compact):(\\d+)(?::(\\d+))?`
   - `SCHEME_EXCEPTION_PATTERN`: `([a-zA-Z]:[\\\\/][^:\\r\\n]+?\\.compact|[^:\\r\\n\\s][^:\\r\\n]*?\\.compact)\\s+line\\s+(\\d+),?\\s+char\\s+(\\d+)`
3. **Multi-Tier File Resolution**:
   - Project directory relative lookup via `ProjectUtil.guessProjectDir(project)` (supporting both `LocalFileSystem` and test `TempFileSystem`).
   - Direct `LocalFileSystem.findFileByPath` with `FileUtil.toSystemIndependentName`.
   - WSL path translation fallback via `CompactToolchainUtil.toWindowsPath`.
   - Drive-letter stripping fallback for synthetic project paths.
4. **Clean Code & Modern Java 25**:
   - Extracted `resolveFromBaseDir` to keep methods $\le$ 40 lines.
   - Preserved file length at $\le$ 200 lines (well under the 400-line ceiling).
   - Zero compilation warnings, zero linter warnings.

---

## 6. Verification & Tests Added

Added test suite `src/test/java/dev/verloren/midnight/run/CompactConsoleFilterTest.java`:
- `testDumbAwareContract`: Asserts `CompactConsoleFilter` implements `DumbAware`.
- `testRelativeDiagnostic`: Verifies standard relative paths convert to 0-based editor line/column offsets.
- `testWindowsAbsolutePathDiagnostic`: Verifies Windows drive letters (`C:/...` and `C:\...`) resolve and hyperlink accurately.
- `testChezSchemeExceptionFormat`: Verifies Chez Scheme internal compiler exceptions are parsed into clickable links.
- `testLineOnlyWithoutColumnDiagnostic`: Verifies diagnostics with line numbers only default column to 0.
- `testUnrelatedOutputIgnored`: Negative assertion ensuring unrelated output returns `null`.

**Verification Harness Result**:
All gates passed with status `"PASSED"` via `scripts/verify-patch.ps1`.

---

## 7. Prevention & Key Lessons Learned

1. **Path Regular Expressions Must Account for Windows**: Any regex matching file paths on Windows must support drive letters (`[a-zA-Z]:[\\/]`).
2. **Console Filters Should Always Implement `DumbAware`**: Console filters that only perform file resolution and text offset calculations have no dependency on PSI indexes and should remain functional during indexing.
3. **Use ProjectUtil for Virtual File Resolution**: Relying exclusively on `LocalFileSystem.getInstance().findFileByPath()` breaks test fixtures using virtual file systems (`TempFileSystem`). Use `ProjectUtil.guessProjectDir()` as the primary entry point.
