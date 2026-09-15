# Sized Data Type Completion Missing Angle Brackets, Caret Positioning, and Size Options

- **Date:** 2026-09-16
- **Feature / Component:** completion / parameterized-types
- **Severity:** Medium
- **Status:** Resolved

## Symptoms

When selecting unsized built-in types such as `Void` or `Boolean` from the autocompletion popup, the type name is inserted cleanly and the cursor advances after the semicolon. However, selecting sized types such as `Bytes` or `Uint` (which require explicit bit-width or byte-length arguments in Compact) merely inserted the bare type name (`Bytes` or `Uint`). The editor did not provide angle brackets, did not place the cursor inside the brackets to accept size input, and did not suggest valid size options (e.g., `32`, `64`, `16`, `8` for `Bytes`, or `8`, `16`, `32`, `64`, `128`, `256` for `Uint`).

## Context

Following the resolution of data type completion popups in declaration live templates, users reported that completing types requiring size definitions (`Bytes`, `Uint`) was suboptimal: it required manual typing of `<>`, manual caret repositioning, and lacked guidance for standard cryptographic byte sizes and integer bit widths.

## Root Cause

1. Built-in type completion in `CompactCompletionContributor` and live template lookup items in `CompactTypeExpression` inserted bare string lookup elements without specialized insert handlers for parameterized types.
2. In active live templates, selecting a lookup element caused `TemplateState.gotoEnd(false)` to position the caret at the variable's tail offset (outside any appended characters), overriding naive insert handlers that repositioned the caret synchronously.
3. The completion classifier `CompactCompletionContext` lacked dedicated context detection for positions inside parameterized type size brackets (`Bytes<...>` and `Uint<...>`), treating them as generic values or expressions rather than size literal contexts.

## Investigation

Inspecting `CompactAngleBraceTypingTest` and `CompactCompletionContributor` demonstrated that while typing `<` manually triggered pair insertion, completing a type name from a lookup list bypassed typing handlers entirely and required an `InsertHandler<LookupElement>`.
Furthermore, testing with live template variables (`$TYPE$`) revealed that `TemplateState` executes post-insertion cleanup that shifts the caret to the end of the template variable unless deferred via `ApplicationManager.getApplication().invokeLater(...)`.
Lastly, `CompactCompletionContext.classify` needed backward token scanning up to the opening `<` while excluding `#` (generic type parameter references) and collection types such as `Vector` (which accept generic element types and length expressions).

## Solution

1. **Created `CompactParameterizedTypeInsertHandler`**:
   - Implements `InsertHandler<LookupElement>`.
   - Appends `<>` (or `<"">` for `Opaque`), places the caret inside `<|>` at `tailOffset + 1`, registers an empty tab scope with `TabOutScopesTracker`, and schedules the auto-popup completion window via `AutoPopupController`.
   - Checks `TemplateManagerImpl.getTemplateState(editor)`. If a live template is active, defers caret repositioning to the next EDT cycle via `ApplicationManager.getApplication().invokeLater(...)` so it runs after `TemplateState.gotoEnd(false)`.
2. **Enhanced `CompactCompletionContext`**:
   - Added `Kind.BYTES_SIZE` and `Kind.UINT_SIZE` context kinds.
   - Implemented `checkParameterizedTypeSizeContext` with backward scanning up to `<` for `Bytes` and `Uint`, properly returning `null` when `#` (generic parameter) or delimiter tokens are encountered.
3. **Updated `CompactCompletionContributor`**:
   - Added `addBytesSizeCompletions` offering prioritized byte sizes (`32`, `64`, `16`, `8`, `48`, `20`).
   - Added `addUintSizeCompletions` offering standard unsigned integer bit widths (`8`, `16`, `32`, `64`, `128`, `256`).
   - Added `createBuiltinTypeLookupElements()` offering both parameterized types (`Bytes`, `Uint`, `Vector`, `Opaque`) and pre-configured variants (`Bytes<32>`, `Bytes<64>`, `Uint<8>` through `Uint<256>`).
4. **Updated `CompactTypeExpression`**:
   - Integrated `CompactCompletionContributor.createBuiltinTypeLookupElements()` into template lookup items to ensure consistent behavior inside live templates.

## Verification

- Added unit tests in `CompactCompletionTest`:
  - `testBytesCompletionInsertsAngleBracketsAndPlacesCaretInside`
  - `testUintCompletionInsertsAngleBracketsAndPlacesCaretInside`
  - `testBytesInsideBracketsSuggestsSizeOptions`
  - `testUintInsideBracketsSuggestsBitWidthOptions`
  - `testBytesSizeOptionCompletion`
  - `testUintSizeOptionCompletion`
  - `testPreconfiguredBytes32Completion`
  - `testPreconfiguredUint64Completion`
- Executed `./gradlew test --tests "dev.verloren.midnight.completion.CompactCompletionTest"` (all 68 tests passing).
- Executed full suite `./gradlew test` (all tests passing with 0 failures, 0 errors).
- Static analysis via `get_file_problems` returned 0 errors across all modified files.

## Prevention / Lesson

When designing completion lookup elements for parameterized types or live templates, account for editor post-selection actions. When live templates manage the caret, deferring cursor adjustments to `invokeLater` ensures the caret stays placed inside parameters rather than being jumped to the end of the template boundary.

## Related Files

- `src/main/java/dev/verloren/midnight/completion/CompactParameterizedTypeInsertHandler.java`
- `src/main/java/dev/verloren/midnight/completion/CompactCompletionContext.java`
- `src/main/java/dev/verloren/midnight/completion/CompactCompletionContributor.java`
- `src/main/java/dev/verloren/midnight/ide/templates/CompactTypeExpression.java`
- `src/test/java/dev/verloren/midnight/completion/CompactCompletionTest.java`

## Related ADRs / Context

- `.ai/bugs/2026-09-16-editor-angle-bracket-pairing.md`
- `.ai/bugs/2026-09-16-template-type-completion-suppression.md`
