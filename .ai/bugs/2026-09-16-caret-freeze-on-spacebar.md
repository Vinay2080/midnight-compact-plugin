# Caret Freeze on First Word and Space Bar from EDT Document Multicaster Indexing and Insert Handler Threading Assertions

- **Date:** 2026-09-16
- **Feature / Component:** toolwindow / completion / threading
- **Severity:** High
- **Status:** Resolved

## Symptoms

After installing the plugin into a production IntelliJ IDEA environment, typing in an editor window (especially after typing the first word of a line, such as `export`, and pressing the Space bar) caused the caret to freeze and refuse to move forward or insert a space. The IDE became unresponsive to typing inputs in `.compact` files, and the `idea.log` contained repeated fatal errors:
1. `java.lang.Throwable: Slow operations are prohibited on EDT. See SlowOperations.assertSlowOperationsAreAllowed javadoc.` originating from `FileTypeIndex.getFiles()` inside `CompactCompilerPanel.findTargetCompactFile()`.
2. `com.intellij.util.concurrency.ThreadingAssertions.createThreadAccessException` originating from `TabOutScopesTracker.getInstance().registerEmptyScopeAtCaret()` inside `CompactParameterizedTypeInsertHandler` and `CompactAssertInsertHandler`.

## Context

In production environments where the "Compact Compiler" tool window is active, `CompactCompilerPanel` registered a global `DocumentListener` via `EditorFactory.getInstance().getEventMulticaster().addDocumentListener(...)`. On every keystroke across any document in the IDE, `documentChanged` was dispatched synchronously on the Event Dispatch Thread (EDT).

## Root Cause

1. **Synchronous Project Indexing and Slow Operations on EDT**:
   On every keypress, `CompactCompilerPanel.documentChanged()` called `findTargetCompactFile()`, which executed `ReadAction.computeBlocking(() -> FileTypeIndex.getFiles(CompactFileType.INSTANCE, GlobalSearchScope.projectScope(project)))` on the EDT. In IntelliJ IDEA, querying file-based indices or running slow operations synchronously on the EDT is strictly prohibited and throws `SlowOperations.assertSlowOperationsAreAllowed()`.
2. **Synchronous Card Rebuild Storms and Disk I/O on EDT**:
   Every keystroke triggered `updateActiveFileInfo()`, which parsed PSI and unconditionally invoked `refreshCards()`. `refreshCards()` executed on the EDT, wiped all UI components (`cardsPanel.removeAll()`), performed disk and WSL checks via `CompactVersionManager.isVersionInstalled(v)` for all 11 compiler versions, and rebuilt all Swing cards, starving the EDT of user typing events.
3. **Missing Write Command Action in Insert Handlers**:
   In `CompactParameterizedTypeInsertHandler` and `CompactAssertInsertHandler`, an `invokeLater` callback repositioned the caret and invoked `TabOutScopesTracker.getInstance().registerEmptyScopeAtCaret(editor)` when a live template was active. Calling `registerEmptyScopeAtCaret()` inside `invokeLater` without a write command action violated `ThreadingAssertions.assertWriteAllowed()`.

## Solution

1. **Guarded and Debounced Document Multicaster in `CompactCompilerPanel`**:
   - Immediately discarded document events if the underlying file is not a `.compact` file.
   - Filtered out edits that are beyond the top 250 characters and do not contain the keyword `"pragma"`.
   - Debounced active file/pragma updates using `com.intellij.util.Alarm` (300ms delay) so rapid typing never triggers synchronous updates or storms.
2. **Eliminated Indexing and Slow Operations on EDT**:
   - Removed the `FileTypeIndex.getFiles()` search from `findTargetCompactFile()`. Only inspect currently selected and open editor files.
   - Guarded card re-renders in `updateActiveFileInfo()`: only invoke `refreshCards()` if the detected pragma constraint or compiler version identifier has actually changed.
3. **Background Version Resolution and Disk I/O**:
   - Moved compiler version discovery, WSL checks, and disk verification in `refreshCards()` off the EDT via `ApplicationManager.getApplication().executeOnPooledThread(...)`, passing immutable card view models to the EDT only for Swing component rendering.
4. **Write Action Scoping in Insert Handlers**:
   - Wrapped `registerEmptyScopeAtCaret()` and caret repositioning in `WriteCommandAction.runWriteCommandAction(project, () -> ...)` inside `CompactParameterizedTypeInsertHandler` and `CompactAssertInsertHandler`.

## Verification

- Added `CompactCompilerPanelTest`:
  - `testFindTargetCompactFileProhibitsSlowOperationsOnEdt`: Verifies that `findTargetCompactFile()` runs cleanly under `SlowOperations.startSection(SlowOperations.ACTION_PERFORM)` without throwing slow operation exceptions.
  - `testFindTargetCompactFileFindsOpenEditor`: Verifies open editor resolution.
  - `testTypingSpaceAfterExportDoesNotFreezeOrThrow`: Simulates typing space after `export` and validates immediate caret advancement without freeze.
- Added `CompactInsertHandlersTest`:
  - Tested `CompactParameterizedTypeInsertHandler` and `CompactAssertInsertHandler` both standalone and with active `TemplateState`, confirming zero `ThreadingAssertions` or write-access violations.
- Full test suite passed 100% with 0 errors and 0 compiler warnings.
