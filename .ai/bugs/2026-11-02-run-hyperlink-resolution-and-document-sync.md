# Bug Record: Compact Run Action Document Desynchronization and Subdirectory Hyperlink Resolution

**Date**: 2026-11-02  
**Subsystem**: Run Configuration / Console Hyperlink Navigation (`dev.verloren.midnight.run`)  
**Affected Component**: `CompactConsoleFilter.java`, `CompactRunProfileState.java`  
**Severity**: High  
**Status**: Resolved  

---

## 1. Symptoms & Failure Behavior

When developers executed Compact smart contract compilation tasks via the Run configuration ("Run 'Compact: ...'"):
1. **Coordinate Desynchronization & Stale Disk File**: When a user modified a `.compact` contract in the editor (for example, introducing or deleting lines, or fixing a syntax error) and immediately clicked "Run", the compiler (`compactc`) processed the older version saved on disk rather than the in-memory editor buffer. Consequently:
   - Line numbers and character coordinates emitted in compiler error messages drifted from the current positions in the editor.
   - The user saw errors referencing lines that had already been altered or removed in the IDE.
2. **Missing Hyperlinks for Subdirectory Contracts**: When compiling contracts located inside subdirectories (such as `manual testing/14_compiler_run_action/CompileTargetContract.compact`), the compiler emitted bare filenames (e.g. `CompileTargetContract.compact:14:1: error: ...`). `CompactConsoleFilter` failed to resolve the bare filename to the actual VirtualFile because it only checked the project root, resulting in zero clickable console hyperlinks.
3. **Range Coordinate Formatting Failures**: Certain compiler diagnostics and warning tools emit range coordinates (e.g. `path.compact:12:5-10` or `path.compact:12:5:12:10`). The previous diagnostic regex only recognized single point coordinates (`:line:col`), causing range diagnostics to either match incorrectly or fail hyperlinking.

---

## 2. Context

During manual integration testing of the Compact compilation run configuration, errors displayed in the run toolwindow console did not auto-navigate or provide clickable hyperlinks when contracts were stored in nested subdirectories. Furthermore, coordinate discrepancies were noticed between in-editor inspections and compiler console output due to dirty document buffers not being committed to the file system prior to running `compactc`.

---

## 3. Root Cause Analysis

1. **Missing Document Buffer Flush (`saveAllDocuments()`)**:
   In IntelliJ IDEA, standard Java and Kotlin run configurations execute a "Make" or "Build" before-run task which calls `FileDocumentManager.getInstance().saveAllDocuments()`. Custom `CommandLineState` runners do not automatically flush dirty document buffers. Because `CompactRunProfileState.startProcess()` invoked `compactc` directly without triggering `saveAllDocuments()`, `compactc` compiled stale disk files while the user's view reflected in-memory `Document` state.
2. **Strict Project Root Resolution for Bare Filenames**:
   `CompactConsoleFilter.resolveFile(path)` relied solely on `baseDir.findFileByRelativePath(path)` and direct string concatenation with `baseDir.getPath()`. If `compactc` reported a relative path or bare filename (e.g., `CompileTargetContract.compact`) for a file residing in a nested package or folder, `findFileByRelativePath` returned `null` and no hyperlink was created.
3. **Missing Compilation Target Context**:
   `CompactConsoleFilter` only accepted `Project project` in its constructor, with no knowledge of the contract file being compiled (`targetFilePath`). Without target file context, relative paths emitted relative to the target's parent folder could not be resolved.
4. **Range Coordinate Regex Incompatibility**:
   `DIAGNOSTIC_PATTERN` did not accommodate range column formats (`:line:col-endcol` or `:line:col:endline:endcol`).

---

## 4. Investigation & Evaluated Approaches

- **IntelliJ Platform SDK Run State Best Practices**:
  - Investigated IntelliJ SDK guidelines for `CommandLineState` and compiler integration. Before-run process execution must ensure all dirty virtual files are flushed to disk synchronously via `FileDocumentManager.getInstance().saveAllDocuments()` on the EDT. If called from a background thread during `startProcess()`, `ApplicationManager.getApplication().invokeAndWait(...)` guarantees completion before the external process spawns.
- **Reference Filter Implementations**:
  - Evaluated `RegexpFilter` and `CargoConsoleFilter` in `intellij-rust`. Rust's compiler filters maintain working directory and target context to resolve relative source paths.
  - Implemented multi-tier file resolution:
    1. Direct target file match (if the emitted filename matches `targetFilePath` or its parent directory relative path).
    2. Project-relative resolution via `baseDir.findFileByRelativePath`.
    3. Direct `LocalFileSystem.findFileByPath` with path normalization.
    4. WSL path translation fallback via `CompactToolchainUtil.toWindowsPath`.
    5. Fallback iterative breadth-first search (BFS) across the project directory tree with depth and file count safeguards to locate any matching `.compact` file.

---

## 5. Solution & Implementation

1. **Synchronous Buffer Flush in `CompactRunProfileState`**:
   ```java
   ApplicationManager.getApplication().invokeAndWait(() -> {
     FileDocumentManager.getInstance().saveAllDocuments();
   });
   ```
   Ensured that all editor buffers are flushed to disk before `compactc` process execution starts.
2. **Context-Aware `CompactConsoleFilter`**:
   - Added constructor `CompactConsoleFilter(@NotNull Project project, @Nullable String targetFilePath)`.
   - Updated `CompactRunProfileState` to pass `configuration.getCompactFilePath()` into `CompactConsoleFilter`.
3. **Enhanced Multi-Tier Resolution in `CompactConsoleFilter`**:
   - Added `resolveTargetFile(path)` to resolve bare filenames or sibling paths against the target file directory.
   - Added `findFileInTree(baseDir, fileName)` using an iterative BFS (`ArrayDeque<VirtualFile>`) with a visited limit of 500 entries to prevent infinite loops or stack overflows while locating `.compact` files in subdirectories.
   - Decomposed resolution into focused delegates (`resolveFile` and `resolveFromSystemPaths`) to strictly comply with the Checkstyle $\le$ 40-line method limit.
4. **Range Coordinate Parsing**:
   - Enhanced `DIAGNOSTIC_PATTERN` regex:
     `"(?:^|(?<=[\\s\\[]))([a-zA-Z]:[\\\\/][^:\\r\\n]+?\\.compact|[^:\\r\\n\\s][^:\\r\\n]*?\\.compact):(\\d+)(?::(\\d+)(?:-\\d+|:\\d+:\\d+)?)?"`
   - Strips range suffixes (e.g. `-10` or `:endline:endcol`) while retaining the 1-based start line and column for `OpenFileHyperlinkInfo`.

---

## 6. Verification Evidence

1. **Reproduction Unit Tests in `CompactConsoleFilterTest.java`**:
   - `testBareFilenameInSubdirectoryResolvedViaTargetFile()`: Confirmed fails without target file context, passes with target file context.
   - `testBareFilenameInSubdirectoryResolvedViaProjectTreeSearch()`: Confirms fallback tree search resolves bare filenames when target file context is absent.
   - `testDiagnosticRangeFormat()`: Confirms range coordinate `counter.compact:12:5-10` is accurately parsed to line 11, col 4 (0-based).
2. **Gate Verification Harness**:
   - Gate 0 (Git Isolation): SUCCESS on `ai/fix-run-hyperlink-and-doc-sync`.
   - Gate 1 (Java 25 Compilation): SUCCESS.
   - Gate 1b (Checkstyle & Banned Imports): SUCCESS (all methods $\le$ 40 lines, file $\le$ 400 lines, 0 banned imports).
   - Gate 2 (Plugin Spec & Compatibility): SUCCESS.
   - Gate 3 (Automated Tests): 10/10 tests in `CompactConsoleFilterTest` pass; 775/775 total tests pass across 69 suites.

---

## 7. Prevention & Lessons Learned

1. **External CLI Tools Require Explicit Disk Sync**: Unlike in-process IDE inspections that parse `PsiFile` in memory, external CLI compilers (`compactc`, `cargo`, `rustc`, `javac`) execute against disk files. Every action or runner that invokes external executables must synchronously call `FileDocumentManager.getInstance().saveAllDocuments()` before launching the process.
2. **Relative Path Resolution Must Be Context-Aware**: CLI compilers often output relative paths or bare filenames relative to the working directory or compiler invocation root. Console filters must be provided with invocation context (e.g., target file path or working directory) rather than assuming paths are always relative to the project root.
3. **Iterative BFS Over Recursive Tree Traversal**: IntelliJ's static analysis inspects `VirtualFile.getChildren()` calls inside recursive methods and flags potential stack overflows. Iterative tree traversal with `ArrayDeque` and visited-node limits eliminates both recursion depth risks and IDE inspection warnings.
