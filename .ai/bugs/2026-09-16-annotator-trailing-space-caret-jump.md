# Caret Jump and Trailing Whitespace Stripping on Spacebar or Indented Empty Lines

- **Date:** 2026-09-16
- **Feature / Component:** annotator / editor
- **Severity:** High
- **Status:** Resolved

## Symptoms

In `.compact` files, users reported that when typing in the editor, the caret position unpredictably jumped to the start of the line (column 0). Specifically:
1. When typing a word and pressing space (`"foo <here>"`), the space was deleted or the caret jumped backward.
2. On an empty line with indentation (e.g. after pressing Enter inside a block), the indentation whitespace was stripped and the caret jumped to the start of the line (column 0).

## Context

In commit `3e109d1`, `CompactExternalAnnotator.collectInformation()` was modified to flush unsaved editor document modifications to disk so that the external CLI compiler (`compactc`) could compile the live buffer rather than stale disk contents. This was implemented via `FileDocumentManager.getInstance().saveDocument(document)` dispatched via `invokeLater` on the Event Dispatch Thread (EDT).

## Root Cause

1. **`saveDocument` Triggers `TrailingSpacesStripper`**: Calling `FileDocumentManager.getInstance().saveDocument(document)` triggers IntelliJ's `beforeDocumentSaving` listeners, including IntelliJ's built-in `TrailingSpacesStripper`.
2. **Stripping Indentation on Empty Lines**: When the user presses Enter to create an indented empty line (e.g. 2 or 4 spaces), the line is initially composed solely of whitespace. The external annotator runs in the background on every keystroke, calls `saveDocument(document)`, and `TrailingSpacesStripper` strips the indentation spaces from underneath the caret, collapsing the line to an empty string and pulling the caret to column 0.
3. **Stripping Space After Typing a Word**: When typing a word followed by space (`"export "`), at that moment the trailing space is at the end of the line. `TrailingSpacesStripper` identifies it as trailing whitespace and deletes it while the user is actively typing.
4. **VFS & I/O Churn**: Invoking `saveDocument` on every keystroke in background highlighting causes continuous disk writes, triggers file watchers, and can lead to infinite daemon restarts.

## Investigation

The issue was traced directly to `CompactExternalAnnotator.java`:
```java
if (docManager.isDocumentUnsaved(document)) {
  ApplicationManager.getApplication().invokeLater(() -> {
    if (!file.getProject().isDisposed() && docManager.isDocumentUnsaved(document)) {
      docManager.saveDocument(document);
    }
  }, ModalityState.nonModal());
}
```
Notice that unit tests failed to catch this bug because it was guarded with `if (!ApplicationManager.getApplication().isUnitTestMode())`. In production, `isUnitTestMode()` was false, causing `saveDocument` to fire on every keystroke.

In the IntelliJ Platform SDK architecture:
- `ExternalAnnotator` must **never** call `FileDocumentManager.saveDocument(document)`.
- When external CLI compilers cannot read from standard input (`stdin`), the standard JetBrains architectural pattern is to capture the live document buffer (`document.getText()`) in `collectInformation()`, create a temporary shadow file in `doAnnotate()`, and run the CLI against that shadow file.

## Solution

1. **Eliminated `saveDocument()` Call**: Removed `FileDocumentManager.saveDocument(document)` and all `invokeLater` invocations from `CompactExternalAnnotator.collectInformation()`.
2. **Live Buffer Capture in `InitialInfo`**: Added `String unsavedContent` to the `CompactExternalAnnotator.InitialInfo` record. If `docManager.isDocumentUnsaved(document)` is true, `unsavedContent` stores `document.getText()` captured under read action.
3. **Shadow File Execution in `doAnnotate()`**:
   - If `info.unsavedContent() != null`, writes the live text to a temporary shadow file in a temp directory.
   - Passes the parent directory of the original source file via `--compact-path` to ensure `compactc` resolves all relative include files.
   - Passes the shadow file path to `compactc`.
   - Cleans up the shadow file directory in `finally`.
4. **Targeted Diagnostic Matching**: `CompactExternalAnnotator.isDiagnosticForFile()` matches both the original file and the shadow file (`file.getName()`).

## Verification

- **Automated Reproduction Test**: Added `testCollectInformationCapturesUnsavedBufferWithoutForcingSaveOrStrippingWhitespace()` in `CompactExternalAnnotatorTest.java`. It verifies that when typing a space on an indented line, the document remains unsaved, the caret does not jump, trailing spaces are retained, and `InitialInfo.unsavedContent()` captures the live buffer.
- **Zero Regressions**: Ran `./gradlew test` across the full test suite (12 actionable tasks executed/up-to-date) with 100% pass rate.

## Prevention / Lesson

- **Never force `saveDocument` in editor annotators or background listeners**: Invoking `FileDocumentManager.saveDocument()` from background daemon passes or typed handlers triggers save hooks, trailing whitespace stripping, and format-on-save plugins, disrupting active typing.
- **Do not hide production-breaking code behind `!isUnitTestMode()`**: Bypassing side-effect code only in unit tests creates a dangerous blind spot where tests pass but production exhibits critical bugs.
- **Use shadow files for external compilers**: For command-line tools without `stdin` support, serialize the in-memory document to a temporary shadow file outside the project structure.

## Related Files

- `src/main/java/dev/verloren/midnight/annotator/CompactExternalAnnotator.java`
- `src/test/java/dev/verloren/midnight/annotator/CompactExternalAnnotatorTest.java`
- `.ai/bugs/README.md`
