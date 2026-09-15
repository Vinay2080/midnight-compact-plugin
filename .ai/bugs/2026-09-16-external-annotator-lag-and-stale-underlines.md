# External Annotator Compilation Lag, Stale Error Underlines, and WSL Path Resolution

- **Date:** 2026-09-16
- **Feature / Component:** annotator / toolchain / runner
- **Severity:** Medium-High
- **Status:** Resolved

## Symptoms

Users reported that even after fixing compiler errors and successfully compiling Compact files, the editor continued to display stale error underlines and error indicators on the top right for an extended time. Diagnostics appeared delayed or out of sync with editor contents. In addition, compiler error diagnostics emitted from WSL environments (`/mnt/<drive>/...`) failed to match Windows project files in IntelliJ, causing diagnostics to be dropped or misattributed.

## Context

The Midnight plugin integrates the `compactc` external compiler as an asynchronous `ExternalAnnotator` to supply deep semantic diagnostics, typechecking, and syntax errors. When files were edited, the annotator executed against disk contents before unsaved editor buffer changes were written to disk, resulting in compiler runs analyzing stale code. Furthermore, after manual compilation execution via run profiles or the toolwindow compiler panel, problems were not automatically cleared.

## Root Cause

1. **Unsaved Document Flushes**: In `CompactExternalAnnotator.collectInformation()`, the annotator ran against `VirtualFile` on disk. If the user had unsaved changes in the active editor buffer, `compactc` read the old file state from disk.
2. **WSL Output Directory & Diagnostic Path Normalization**: In WSL toolchains, temporary directory outputs need to reside in native Linux `/tmp` rather than Windows temp paths. Additionally, diagnostics emitted by `compactc` under WSL start with `/mnt/<drive>/...`, whereas IntelliJ's `VirtualFile.getPath()` on Windows returns paths with Windows drive letters (or leading `/` in `LightVirtualFile`), causing path comparison failures.
3. **Run Profile & Tool Window Process Completion Listeners**: When a compilation run completed in the `CompactRunProfileState` or `CompactCompilerPanel`, problems were not proactively cleared and the daemon analyzer was not triggered to refresh.
4. **Empty Line / Boundary Text Range Handling**: Annotations on empty lines or past the line boundary failed to clamp safely to valid document offsets.

## Solution

1. **Buffer Synchronization in `CompactExternalAnnotator`**:
   - Flushed unsaved editor documents to disk asynchronously via `FileDocumentManager.getInstance().saveDocument(document)` inside `collectInformation()` before initiating compiler annotation.
2. **WSL Path Mapping & Safe Normalization**:
   - Added `CompactToolchainUtil.toWindowsPath()` to translate `/mnt/<drive>/...` paths to Windows drive paths (`C:\...`).
   - Enhanced `CompactExternalAnnotator.isDiagnosticForVirtualFile()` to normalize both WSL paths and leading slash drive prefixes (`/C:/...` -> `C:/...`).
3. **Execution Completion Problem Refresh**:
   - Attached a `ProcessListener` in `CompactRunProfileState` and `CompactCompilerPanel` to invoke `CompactProblemUtil.clearProblemsAndRestart()` upon compiler process termination.
4. **Resilient Range Calculation**:
   - Clamped line and offset boundaries in `CompactExternalAnnotator.getRange()` to prevent `IndexOutOfBoundsException` on empty lines or trailing whitespace.

## Verification

- `CompactExternalAnnotatorTest`: Added and verified `testIsDiagnosticForVirtualFileWslPath` and `testTextRangeCalculationHandlesEmptyLineAndBoundaries`.
- Complete test suite passed (`BUILD SUCCESSFUL in 1m 37s`, 12 actionable tasks executed/up-to-date).
