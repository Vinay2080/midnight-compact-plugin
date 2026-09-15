# ADR-029: Angle Bracket Pairing, Overtyping, and Parameterized Type Scaffolding

## Status
Accepted (v1.2.6 / Complete)

## Date
2026-09-16

## Subsystem
Editor & Completion

## Related ADRs
- [ADR-006](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/ADR-006-non-destructive-smart-enter-completion.md): Non-Destructive Intent-Preserving Smart Enter Completion
- [ADR-009](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/ADR-009-compact-type-system-grammar-alignment.md): Strict Alignment with Upstream Compact Type Grammar
- [ADR-019](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/ADR-019-context-aware-code-completion.md): Context-Aware Code Completion & Structural Scoping
- [ADR-025](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/ADR-025-compact-live-templates-and-surround-with.md): Parametric Live Templates & Surround-With Statement Wrappers
- [ADR-026](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/ADR-026-compact-export-ledger-completion-and-templates.md): Export Ledger Code Completion, Contextual Filtering, & Parametric Live Templates
- [ADR-027](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/ADR-027-compact-declaration-name-generator-and-templates.md): Universal Declaration Name Auto-Numbering, Scope Analysis, & Live Template Macros

---

## 1. Context & Problem Statement
In the Compact smart contract language, type arguments and size specifiers are expressed with angle brackets (`<...>`). These appear extensively across:
1. **Parameterized & Sized Primitive Types**:
   - `Bytes<n>` (e.g. `Bytes<32>`)
   - `Uint<n>` (e.g. `Uint<8>`, `Uint<16>`, `Uint<32>`, `Uint<64>`, `Uint<128>`, `Uint<256>`)
   - `Vector<T, n>` (e.g. `Vector<Field, 4>`)
   - `Opaque<"tag">`
   - `Field`, `Boolean`
2. **Parameterized Builtin Expressions**:
   - `default<T>()`
   - `slice<start, length>(expr)`
3. **Standard Library Generic Collections**:
   - `Map<K, V>`, `Set<T>`, `Cell<T>`, `List<T>`
4. **Generic Declaration Headers**:
   - `circuit name<T>(...)`, `witness name<T>(...)`, `struct Name<T>`, `type Name<T>`, `module Name<T>`, `contract Name<T>`

Prior to this implementation, the editor suffered from several critical developer experience deficiencies:
1. **No Automatic Angle Bracket Pairing**: Typing `<` after generic types or expressions did not auto-close with `>` or position the caret inside (`<|>`), forcing developers to manually type both brackets.
2. **Overtyping Collision & Redundant Syntax Errors**: If a closing `>` was present, typing `>` inserted a redundant character (`<>>`), corrupting the syntax and requiring manual backspacing.
3. **Orphaned Closing Brackets on Backspace**: Pressing Backspace when the caret was positioned between matching angle brackets (`<|>`) deleted only `<` while leaving an orphaned `>` in the document.
4. **False-Positive Pairing in Comparison Expressions**: Because `<` is also the binary "less-than" comparison operator (`a < b`), naive bracket matching would erroneously insert `>` in boolean expressions.
5. **Bare Sized Type Insertion During Code Completion**: Selecting `Bytes` or `Uint` from code completion inserted bare tokens (`Bytes`, `Uint`) lacking the mandatory bit-width/size parameter, which violates the upstream compiler specification and immediately triggered compiler diagnostics.
6. **Live Template Premature Ejection**: During live template expansion (e.g. `ledger $NAME$: $TYPE$;`), selecting a parameterized type from completion caused IntelliJ's `TemplateState` to advance to the end of the template line, placing the caret after the semicolon rather than inside `<|>` for size specification.

---

## 2. Decision Drivers & Constraints
- **Compact Language Grammar & Type Rules**: `Bytes` and `Uint` in Compact require size arguments; bare `Bytes` is invalid according to `compact/compiler/langs.ss`.
- **IntelliJ Platform Typing Contracts**:
  - `TypedHandlerDelegate` handles character insertion hooks on the Event Dispatch Thread (EDT).
  - `BackspaceHandlerDelegate` handles backspace events prior to and following character deletion.
  - `InsertHandler<LookupElement>` orchestrates post-selection document mutations and caret positioning.
- **Negative Context Filtering**: Pairing must be strictly suppressed inside comments, string literals, mid-identifier typing, and following non-type tokens.
- **Template Concurrency**: Live template evaluation must not override or displace caret placement within parameterized brackets.
- **Performance & Responsiveness**: Typing handlers execute on every keystroke on the EDT. They must use lightweight lexer token lookback via `HighlighterIterator` and never trigger full AST re-parses.

---

## 3. Evaluated Options

### Option 1: Standard IntelliJ Brace Matching (`PairedBraceMatcher`)
- **Description**: Register `<` and `>` as paired braces in `CompactBraceMatcher`.
- **Pros**: Minimal code; standard platform mechanism.
- **Cons**: Severe failure mode. Because `<` and `>` serve as binary comparison operators (`x < y`), registering them in `PairedBraceMatcher` causes IntelliJ to auto-insert `>` during ordinary mathematical and logical comparisons inside `if`, `while`, and circuit expressions.

### Option 2: Full PSI-Tree Inspection on Keystroke
- **Description**: Parse the PSI tree in `TypedHandlerDelegate` to evaluate whether the offset represents a generic type position.
- **Pros**: Complete syntactic awareness.
- **Cons**: Unacceptable performance penalty on the EDT. Keystroke latency degrades noticeably, and in-flight incomplete code produces `PsiErrorElement` nodes that prevent valid syntactic inference before the bracket is closed.

### Option 3: Specialized `TypedHandlerDelegate` & `BackspaceHandlerDelegate` with `HighlighterIterator` Lookback and `InsertHandler` (Selected)
- **Description**:
  1. Inspect preceding tokens using non-allocating `HighlighterIterator` lookback.
  2. Auto-pair `<|>` only when preceded by verified parameterized types, builtin expressions (`default`, `slice`), type identifiers (PascalCase or single uppercase `T`), or generic declaration keywords.
  3. Calculate local bracket balance within the statement to enable intelligent overtyping step-over.
  4. Intercept backspace to remove the matching `>` when empty (`<|>`).
  5. Provide `CompactParameterizedTypeInsertHandler` to append `<>`, place caret inside, register tab-out scope, and trigger size option popups.
- **Pros**: Zero UI lag, 100% accurate context isolation, zero comparison operator interference, full live template integration.
- **Cons**: Requires dedicated typing test coverage across varied whitespace and expression contexts.

---

## 4. Decision Outcome
Adopted **Option 3**.

### 1. Angle Bracket Auto-Closing (`CompactAngleBraceTypedHandler`)
- Registered under `<typedHandler>` in `plugin.xml`.
- In `beforeCharTyped`:
  - When `<` is typed and `shouldComplete(editor)` returns `true`, sets `OPENING_TYPED_KEY` user data on the editor.
  - When `>` is typed immediately preceding an existing `GT` token and `calculateBalance(editor) == 0`, consumes the event (`Result.STOP`) and advances the caret offset by 1 (overtyping step-over).
- In `charTyped`:
  - If `OPENING_TYPED_KEY` is present and `calculateBalance(editor) == 1`, inserts `>` immediately following the caret, placing the caret cleanly inside `<|>`.
- In `shouldComplete`:
  - Checks `CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET`.
  - Rejects active selections.
  - Rejects comments (`LINE_COMMENT`, `BLOCK_COMMENT`, `UNTERMINATED_BLOCK_COMMENT`) and strings (`STRING_LITERAL`, `UNTERMINATED_STRING`).
  - Matches built-in parameterized tokens: `VECTOR_TYPE`, `UINT_TYPE`, `BYTES_TYPE`, `OPAQUE_TYPE`, `FIELD_TYPE`, `BOOLEAN_TYPE`.
  - Matches parameterized builtin expressions: `DEFAULT`, `SLICE`.
  - Matches identifiers preceded by generic declaration keywords (`circuit`, `witness`, `struct`, `enum`, `type`, `module`, `contract`).
  - Matches type-like identifiers (PascalCase or single-letter type parameters like `T`, `K`, `V`).

### 2. Balanced Backspace Deletion (`CompactAngleBraceBackspaceHandler`)
- Registered under `<backspaceHandlerDelegate>` in `plugin.xml`.
- In `beforeCharDeleted`:
  - When backspacing `<` immediately preceding `>`, flags `BACKSPACE_ENABLED_KEY`.
- In `charDeleted`:
  - If enabled and `calculateBalance(editor) < 0`, deletes the trailing `>` character.

### 3. Parameterized Type Completion (`CompactParameterizedTypeInsertHandler`)
- Automatically appends `<>` (or `<"">` for `Opaque`), repositions the caret inside, registers empty scope with `TabOutScopesTracker`, and schedules auto-popup via `AutoPopupController.getInstance(project).scheduleAutoPopup(editor)`.
- Defeats premature live template ejection by scheduling caret repositioning via `ApplicationManager.getApplication().invokeLater(...)` when an active `TemplateState` is present.

### 4. Interactive Live Template Type Macro (`CompactTypeMacro` & `CompactTypeExpression`)
- Registered under `<liveTemplateMacro>` as `compactType()` in `plugin.xml`.
- Provides interactive dropdown lists of all Compact primitive types, sized types, and project-in-scope types during live template tab-stops.

---

## 5. Compiler Ground Truth & Reference Plugin Citations
- **Compact Compiler Nanopass AST**: [`compact/compiler/langs.ss`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/compact/compiler/langs.ss) &mdash; Defines `(Bytes ,expr)` and `(Uint ,expr)`.
- **Compact Compiler Parser**: [`compact/compiler/parser.ss`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/compact/compiler/parser.ss) &mdash; Type expressions and generic syntax for contracts, modules, and circuits.
- **Reference Implementation (`intellij-rust`)**:
  - `org.rust.ide.typing.RsAngleBracketTypedHandler`: Reference implementation of `TypedHandlerDelegate` for Rust generic type parameters `<T>`.
  - `org.rust.ide.typing.RsAngleBracketBackspaceHandler`: Reference implementation of `BackspaceHandlerDelegate`.

---

## 6. Feature Implementation Map
- Typed Handler: [`CompactAngleBraceTypedHandler.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/editor/CompactAngleBraceTypedHandler.java)
- Backspace Handler: [`CompactAngleBraceBackspaceHandler.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/editor/CompactAngleBraceBackspaceHandler.java)
- Parameterized Insert Handler: [`CompactParameterizedTypeInsertHandler.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/completion/CompactParameterizedTypeInsertHandler.java)
- Live Template Type Macro: [`CompactTypeMacro.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/ide/templates/CompactTypeMacro.java)
- Live Template Type Expression: [`CompactTypeExpression.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/ide/templates/CompactTypeExpression.java)
- Completion Contributor: [`CompactCompletionContributor.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/completion/CompactCompletionContributor.java)
- Extension Registrations: [`plugin.xml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/resources/META-INF/plugin.xml)
- Unit Tests:
  - [`CompactAngleBraceTypingTest.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/editor/CompactAngleBraceTypingTest.java) (24 unit tests)
  - [`CompactCompletionTest.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/completion/CompactCompletionTest.java) (70 unit tests)

---

## 7. Consequences & Trade-Offs
- **Positive**:
  - Smooth, modern editor experience for all generic types, sized types, and collection data structures.
  - Zero accidental pairing on binary comparisons (`a < b`).
  - Zero syntax corruption from overtyping `>`.
  - Sized types (`Bytes<32>`, `Uint<64>`) automatically insert valid brackets with inline parameter suggestions.
- **Trade-Offs**:
  - Unconventional multi-line generic signatures without closing brackets fall back gracefully to standard single-character typing without pairing.
