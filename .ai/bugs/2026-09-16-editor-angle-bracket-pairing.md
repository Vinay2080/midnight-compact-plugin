# Missing Angle Bracket (<>) Auto-Closing, Cursor Placement, and Overtyping

- **Date:** 2026-09-16
- **Feature / Component:** editor / brace-matching
- **Severity:** Medium
- **Status:** Resolved

## Symptoms

When typing parenthesis `(`, curly brace `{`, or square bracket `[` in the Compact language editor, the IntelliJ Platform automatically inserts the closing delimiter (`)`, `}`, `]`) and places the cursor inside the pair (`(|)`, `{|}`). However, when typing `<` after generic types (e.g. `Vector`, `Uint`, `Bytes`, `Map`), expressions (`default`, `slice`), or declaration headers (`circuit foo`, `struct Bar`), the editor did not mirror the closing angle bracket `>` or place the cursor inside (`<|>`). Furthermore, typing `>` when positioned immediately before a closing `>` inserted a duplicate `>` rather than stepping over, and backspacing `<` did not remove the paired `>`.

## Context

User identified that bracket pairing worked for parentheses, brackets, and curly braces, but failed for angle brackets:
*"when user types ( or { r { it mirrors the that bracket and place the cursor inside to type the required thing not the same case for < find the bug and fix it"*

## Root Cause

In `CompactPairedBraceMatcher.java`, angle brackets were registered in `getPairs()` via:
```java
new BracePair(CompactTokenTypes.LT, CompactTokenTypes.GT, false)
```
with class Javadoc stating that it *"Enables editor delimiter highlighting, auto-insertion of closing braces, and structural navigation"*.

However, in the IntelliJ Platform, `PairedBraceMatcher` only handles structural navigation (`Ctrl+Shift+M`) and matching brace highlighting in the editor. The platform's built-in `TypedHandler.inEditorType` deliberately does **not** auto-insert `>` when typing `<`. Because `<` is also a binary comparison operator (less-than) in C-style and ALGOL-derived languages, the platform restricts automatic delimiter insertion in `TypedHandler` exclusively to `)`, `}`, `]`, and quote marks.

Supporting angle bracket auto-completion (`<|>`), overtyping step-over on `>`, and paired backspace deletion in an IntelliJ language plugin requires custom implementations of `TypedHandlerDelegate` and `BackspaceHandlerDelegate` registered as extension points in `plugin.xml`.

## Investigation

1. **Inspection of `CompactPairedBraceMatcher.java`**:
   Verified that `BracePair(LT, GT, false)` was present and active. However, typing `<` in test fixtures never auto-inserted `>`.
2. **Analysis of IntelliJ Platform Core**:
   Inspected `com.intellij.codeInsight.editorActions.TypedHandler`. Discovered that `<` is not included in the hardcoded set of characters handled by `inEditorType` or `handleAfterBraceTyped`.
3. **Reference Implementations**:
   Examined how official JetBrains plugins (such as `intellij-rust`, `intellij-community` Java/Kotlin handlers) implement angle bracket pairing. Rust implements this via `RsAngleBraceHandlers.kt` / `RsBraceHandlers.kt` using `TypedHandlerDelegate` and `BackspaceHandlerDelegate`.
4. **Compact Grammar Specification Analysis**:
   Consulted the Compact compiler grammar (`compact/compiler/parser.ss`) to identify all syntactic positions where `<` denotes a type parameter or type argument rather than a binary operator:
   - Built-in parameterized types: `Vector`, `Uint`, `Bytes`, `Opaque`, `Field`, `Boolean`.
   - Built-in parameterized expressions: `default`, `slice`.
   - Generic declarations: `circuit <name><`, `witness <name><`, `struct <name><`, `enum <name><`, `type <name><`, `module <name><`, `contract <name><`.
   - Type references: Standard PascalCase type names (`Map`, `Set`, `List`, `Cell`) and single uppercase generic type parameters (`T`, `U`, `V`, `K`), excluding ALL_CAPS constants.
   - Suppression: Comments (line and block) and string literals must never trigger bracket pairing.
   - Caret position: Typing `<` in the middle of an identifier must not trigger pairing.

## Solution

1. **Created `CompactAngleBraceTypedHandler`**:
   - Implemented `TypedHandlerDelegate`.
   - In `beforeCharTyped`:
     - When `<` is typed: Checks `CompactFile`, verifies `CodeInsightSettings.AUTOINSERT_PAIR_BRACKET`, ensures no text selection, and calls `shouldComplete(editor)`. If eligible, sets `OPENING_TYPED_KEY` user data on the editor.
     - When `>` is typed: Checks if the character immediately ahead of the caret is `>` (`CompactTokenTypes.GT`) and that the statement angle bracket balance is 0 (`calculateBalance(editor) == 0`). If balanced, advances the caret by one offset (`moveToOffset(offset + 1)`) and returns `Result.STOP` to step over without inserting a duplicate `>`.
   - In `charTyped`:
     - If `OPENING_TYPED_KEY` is present, verifies that after `<` has been inserted, the statement angle bracket balance is 1. If so, inserts `>` at the caret position and positions the caret between them (`<|>`).
   - Implemented `shouldComplete`:
     - Suppresses completion inside comments (`PlatformPatterns.psiComment()`) and string literals.
     - Detects preceding keywords and tokens: built-in types (`Vector`, `Uint`, `Bytes`, `Opaque`, `Field`, `Boolean`), expressions (`default`, `slice`).
     - Detects preceding identifiers if preceded by generic declaration keywords (`circuit`, `witness`, `struct`, `enum`, `type`, `module`, `contract`) or if matching type identifier patterns (PascalCase or single uppercase generic parameter like `T`).
   - Implemented `calculateBalance`:
     - Scans backward within the statement boundary (delimited by `{`, `}`, `;`) counting unclosed `<` (+1) vs `>` (-1) using the lexer.
2. **Created `CompactAngleBraceBackspaceHandler`**:
   - Implemented `BackspaceHandlerDelegate`.
   - In `beforeCharDeleted`: When deleting `<` if the next character is `>`, sets `BACKSPACE_ENABLED_KEY` on the editor.
   - In `charDeleted`: Checks whether the angle bracket balance is negative (unmatched closing `>`). If so, deletes the matching `>` at the caret position.
3. **Registered Handlers in `plugin.xml`**:
   Registered `<typedHandler>` and `<backspaceHandlerDelegate>` under `<extensions defaultExtensionNs="com.intellij">`.

## Verification

Created test suite `dev.verloren.midnight.editor.CompactAngleBraceTypingTest` with 24 comprehensive unit tests:
- Type `<` after `Vector` -> `Vector<<caret>>`
- Type `<` after `Uint` -> `Uint<<caret>>`
- Type `<` after `Bytes` -> `Bytes<<caret>>`
- Type `<` after `Opaque` -> `Opaque<<caret>>`
- Type `<` after `default` -> `default<<caret>>`
- Type `<` after `slice` -> `slice<<caret>>`
- Type `<` after type identifier `Map` -> `Map<<caret>>`
- Type `<` after generic parameter `T` -> `T<<caret>>`
- Type `<` in generic declarations: `circuit foo<`, `witness bar<`, `struct Box<`, `type Alias<`, `module Mod<`, `contract Cont<` -> mirrored and caret placed inside
- Nested generic type: `Vector<Uint<caret>>` -> `Vector<Uint<<caret>>>`
- Step-over typing `>`: `Vector<<caret>>` + `>` -> `Vector<><caret>` (moves cursor, no duplicate)
- Paired backspace: `Vector<<caret>>` + `\b` -> `Vector<caret>` (deletes both)
- Negative tests: comparison `if (a <caret>)` does NOT pair, inside identifier `struct Fo<caret>o` does NOT pair, constant `const CONSTANT<caret>` does NOT pair, in line comment does NOT pair, in block comment does NOT pair, in string literal does NOT pair, when `AUTOINSERT_PAIR_BRACKET` is disabled does NOT pair.

All 24 unit tests passed. Full test suite ran with zero regressions.

## Prevention / Lesson

In IntelliJ plugin development, `PairedBraceMatcher` only enables delimiter highlighting and structural navigation. It never performs typed character insertion for angle brackets because `<` doubles as a binary operator. Any language supporting generics or type parameters must explicitly implement `TypedHandlerDelegate` and `BackspaceHandlerDelegate` to provide bracket pairing, overtyping, and deletion semantics.

## Related Files

- `src/main/java/dev/verloren/midnight/editor/CompactAngleBraceTypedHandler.java`
- `src/main/java/dev/verloren/midnight/editor/CompactAngleBraceBackspaceHandler.java`
- `src/main/java/dev/verloren/midnight/editor/CompactPairedBraceMatcher.java`
- `src/main/resources/META-INF/plugin.xml`
- `src/test/java/dev/verloren/midnight/editor/CompactAngleBraceTypingTest.java`

## Related ADRs / Context

- `ADR-004: Code Style, Formatting, and Indentation Engine`
