# ADR-030: Universal Delimiter and Structural Symbol Skipping During Editor Typing

## Status
Accepted (v1.2.7 / Complete)

## Date
2026-09-16

## Subsystem
Editor & Typing

## Related ADRs
- [ADR-006](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/ADR-006-non-destructive-smart-enter-completion.md): Non-Destructive Intent-Preserving Smart Enter Completion
- [ADR-009](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/ADR-009-compact-type-system-grammar-alignment.md): Strict Alignment with Upstream Compact Type Grammar
- [ADR-019](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/ADR-019-context-aware-code-completion.md): Context-Aware Code Completion & Structural Scoping
- [ADR-029](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/ADR-029-angle-bracket-pairing-and-parameterized-type-scaffolding.md): Angle Bracket Pairing, Overtyping, and Parameterized Type Scaffolding

---

## 1. Context & Problem Statement
In the Compact smart contract language, declarations, type annotations, bindings, function headers, collection literals, and block statements rely heavily on closing delimiters and structural punctuation symbols. For example:
```compact
witness localSk(): Bytes<32>;
circuit verifyTx(tx: Transaction, index: Uint<32>): Boolean {
    let mapping = { id: 1, tag: "contract" };
    let values = [1, 2, 3];
}
```

Prior to this implementation, typing in the editor suffered from frequent collision and syntax corruption when developers typed matching delimiters or punctuation symbols that were already present ahead of the caret:
1. **Parenthesis Collision**: In `witness localSk(<caret>): Bytes<32>;`, when a developer typed `)`, standard typing inserted a second parenthesis resulting in `witness localSk()): Bytes<32>;`, creating an immediate syntax error.
2. **Colon Duplication**: In `witness localSk()<caret>: Bytes<32>;`, typing `:` inserted a duplicate colon `::`, which violates Compact syntax because Compact does not possess a double-colon `::` token.
3. **Closing Brace & Bracket Collision**: In vector literals `arr[<caret>]` or struct/block definitions `{ <caret> }`, typing `]` or `}` inserted duplicate closing delimiters (`[]]` or `{}}`).
4. **Closing Quote Collision**: In string literals `"hello<caret>"` or `'tag<caret>'`, typing `"` or `'` inserted an additional quote (`"""`), corrupting the string token.
5. **Semicolon & Comma Duplication**: In statements like `Bytes<32><caret>;` or argument lists `foo(a<caret>, b)`, typing `;` or `,` inserted redundant punctuation (`;;` or `,,`).

Developers expect that when the caret is positioned immediately before a closing delimiter or structural symbol, typing that character cleanly advances the caret past the symbol (overtyping / skip) rather than duplicating it.

---

## 2. Decision Drivers & Constraints
- **Compact Grammar Ground Truth**: Compact defines distinct tokens for delimiters and structural punctuation (`RPAREN`, `RBRACKET`, `RBRACE`, `GT`, `COLON`, `SEMICOLON`, `COMMA`, `STRING_LITERAL`). Compact does not support `::` or `;;` or `,,` as valid syntax tokens.
- **IntelliJ Platform Typing Architecture**:
  - Interceptions occur in `TypedHandlerDelegate.beforeCharTyped` on the Event Dispatch Thread (EDT).
  - Performance must be $O(1)$ and zero-allocation on non-matching keystrokes to ensure zero UI latency.
  - Context isolation: Delimiter skipping must NEVER activate inside comments (`LINE_COMMENT`, `BLOCK_COMMENT`) or within arbitrary string literal contents.
  - Active selection semantics: If text is selected (`editor.getSelectionModel().hasSelection()`), typing must replace the selection per standard platform conventions.
  - User preferences: Closing bracket skipping must respect `CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET`, and quote skipping must respect `AUTOINSERT_PAIR_QUOTE`.

---

## 3. Evaluated Options

### Option 1: Rely Exclusively on Default Platform `PairedBraceMatcher` & `QuoteHandler`
- **Description**: Rely on IntelliJ's internal `TypedHandler.handleRParen` and `SimpleTokenSetQuoteHandler`.
- **Pros**: Zero additional plugin code.
- **Cons**: Severe functional deficits. The platform has zero built-in support for punctuation step-over (`:`, `;`, `,`), leading to syntax errors whenever typing `:` or `;` in templates. Furthermore, IntelliJ's internal bracket step-over frequently fails when code was generated via live templates, file templates, or non-immediate typing sessions.

### Option 2: Full PSI-Tree Parsing on Keystroke
- **Description**: Re-parse the AST at the caret on every keystroke to inspect parent PSI elements.
- **Pros**: Complete syntactic AST awareness.
- **Cons**: Unacceptable performance penalty and UI freezing on the EDT during rapid typing. In-flight incomplete code produces `PsiErrorElement` nodes that impede reliable inference.

### Option 3: Dedicated `CompactDelimiterTypedHandler` with `HighlighterIterator` Verification (Selected)
- **Description**:
  1. Inspect the character immediately ahead of the caret in $O(1)$ time via `editor.getDocument().getCharsSequence().charAt(offset)`.
  2. If the character matches `c`, inspect the underlying lexer token type via lightweight `HighlighterIterator`.
  3. Validate that the token matches the expected `IElementType` (`RPAREN`, `RBRACKET`, `RBRACE`, `GT`, `COLON`, `SEMICOLON`, `COMMA`), or is the terminal closing quote of a `STRING_LITERAL`.
  4. If validated, advance the caret by 1 offset (`editor.getCaretModel().moveToOffset(offset + 1)`) and return `Result.STOP`.
- **Pros**: Zero UI lag, 100% reliable across templates and pre-existing code, zero false positives inside comments or string interiors, complete coverage of delimiters and symbols.
- **Cons**: Requires dedicated unit test fixtures covering all supported symbols and negative boundary contexts.

---

## 4. Decision Outcome
Adopted **Option 3**.

### Implementation Details: `CompactDelimiterTypedHandler`
- Registered under `<typedHandler>` in `plugin.xml`.
- Supported character mappings:
  - `)` $\to$ `CompactTokenTypes.RPAREN`
  - `]` $\to$ `CompactTokenTypes.RBRACKET`
  - `}` $\to$ `CompactTokenTypes.RBRACE`
  - `>` $\to$ `CompactTokenTypes.GT`
  - `:` $\to$ `CompactTokenTypes.COLON`
  - `;` $\to$ `CompactTokenTypes.SEMICOLON`
  - `,` $\to$ `CompactTokenTypes.COMMA`
  - `"` and `'` $\to$ `CompactTokenTypes.STRING_LITERAL` (when `offset == iterator.getEnd() - 1`)
- Execution flow:
  1. Validate `file instanceof CompactFile` and no active selection.
  2. Perform fast boundary check: `offset < chars.length() && chars.charAt(offset) == c`. If false, immediately return `Result.CONTINUE`.
  3. Create `HighlighterIterator` at `offset`.
  4. For bracket tokens (`)`, `]`, `}`, `>`), verify `CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET`.
  5. For quote tokens (`"`, `'`), verify `CodeInsightSettings.getInstance().AUTOINSERT_PAIR_QUOTE`.
  6. For punctuation (`:`, `;`, `,`), execute unconditional step-over.
  7. Advance caret to `offset + 1` and return `Result.STOP`.

---

## 5. Compiler Ground Truth & Reference Plugin Citations
- **Compact Compiler Parser**: [`compact/compiler/parser.ss`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/compact/compiler/parser.ss) &mdash; Lines 100-300: Declaration headers and punctuation rules.
- **Compact Compiler Lexer**: [`compact/compiler/lexer.ss`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/compact/compiler/lexer.ss) &mdash; Lines 150-220: Delimiters, colons, and semicolons.
- **Reference Implementations**:
  - `intellij-rust`: `RsBraceHandlers.kt` &mdash; Uses `HighlighterIterator` lookahead with `EditorModificationUtil.moveCaretRelatively(editor, 1)` and `Result.STOP`.
  - `intellij-scala`: `ScalaTypedHandler.scala` &mdash; Delimiter and symbol overtyping.

---

## 6. Feature Implementation Map
- Typed Handler: [`CompactDelimiterTypedHandler.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/editor/CompactDelimiterTypedHandler.java)
- Extension Registration: [`plugin.xml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/resources/META-INF/plugin.xml)
- Unit Tests: [`CompactDelimiterTypingTest.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/editor/CompactDelimiterTypingTest.java)

---

## 7. Consequences & Trade-Offs
- **Positive**:
  - Eliminates syntax corruption when typing over closing delimiters and punctuation in Compact code.
  - Seamlessly handles sequential typing, such as typing `)` then `:` in `witness localSk(): Bytes<32>;`.
  - Zero latency impact on general typing.
  - Respects user settings and suppresses skipping inside comments and strings.
- **Trade-Offs**:
  - If a developer genuinely wants to insert a duplicate colon or semicolon (neither of which is valid in Compact), they must paste it or press right-arrow.
