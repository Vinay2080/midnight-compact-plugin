# ADR-006: Non-Destructive Intent-Preserving Smart Enter Completion

## Status
Accepted (Implemented in Phase 28 / v1.2.4)

## Context & Problem Statement
The "Complete Current Statement" / "Smart Enter" action (`Ctrl+Shift+Enter` / `Cmd+Shift+Enter`) in IntelliJ is designed to insert syntax completions (parentheses, braces, semicolons) and move the caret into the next logical position.
In initial implementations, naive heuristics caused two major issues:
1. When a developer typed a trailing colon on a circuit signature (e.g., `circuit transfer(to: Address):`), Smart Enter forced `: Void`, overwriting the developer's explicit decision to provide a return type.
2. When a developer typed an incomplete `const` declaration (e.g. `const x` or `const x: Field`), Smart Enter prematurely appended `;` (`const x;`), producing invalid Compact code because `const` bindings strictly require an initializer expression (`=`).
3. For incomplete types like `Byte`, an erroneous `Bytes[]` was suggested based on Java/Rust conventions instead of the Compact specification.

## Authoritative References
1. **Compact Upstream Compiler Parser** ([`compact/compiler/parser.ss:624-656`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/compact/compiler/parser.ss#L624-L656)):
   - Type definitions for `Uint` and `Bytes`:
     ```scheme
     [type-unsigned-integer-bits :: src (KEYWORD Uint) #\< tsize #\> => ...]
     [type-bytes :: src (KEYWORD Bytes) #\< tsize #\> => ...]
     ```
   - Compact types **strictly use angle brackets `<...>`**, e.g., `Bytes<32>`, `Uint<64>`, `Vector<N, T>`.
   - `const` statements in Compact grammar:
     ```scheme
     [statement-const :: src (KEYWORD const) (SEP+ cbinding #\, #f) #\; => ...]
     ```
     where `cbinding` is `(src pattern type expr)` — requiring an initializer expression.
2. **Production JetBrains Implementations**:
   - `intellij-rust`: [`RsSmartEnterProcessor.kt`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/intellij-rust/src/main/kotlin/org/rust/ide/typing/assist/RsSmartEnterProcessor.kt)
   - `intellij-scala`: [`ScalaSmartEnterProcessor.scala`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/intellij-scala/scala/scala-impl/src/org/jetbrains/plugins/scala/editor/smartEnter/ScalaSmartEnterProcessor.scala)
   - Both reference processors guard incomplete declarations from premature semicolon termination and position the cursor at the missing operand/initializer.

## Decision
1. **Trailing Colon Preservation**:
   If a circuit signature ends with `:`, format to `: ` (single space), position the caret directly at the return type slot, and trigger `AutoPopupController.getInstance(project).scheduleAutoPopup(editor)` rather than injecting `Void`. Only when `:` is completely omitted after `)` is `: Void {\n  \n}` inserted (since omitted returns in Compact default to `Void`).
2. **Grammar-Accurate Type Completion**:
   Incomplete types (`Byte`, `Bytes`, `Uint`, `Vector`) complete to `<>` with the caret positioned inside the angle brackets (`Bytes<<caret>>`, `Uint<<caret>>`).
   Unclosed angle brackets (e.g. `Bytes<32`) automatically close with `>` and append the body `{ ... }`.
3. **Const Statement Protection**:
   If a `const` statement lacks an `=` operator, append ` = ` and position the caret after it for immediate value typing with auto-popup. Only append `;` when an expression is present.
4. **Bare Keyword Guards**:
   Bare keywords (`contract`, `struct`, `enum`, `module`, `circuit`, `constructor`) without an identifier append a space and wait for the name rather than appending an invalid `;`.

## Scalability & Anti-Hardcoding Evaluation
- **Is it hardcoded?**: No. It maps directly to grammar states:
  - Header with no colon -> default `: Void`
  - Header with colon -> await return type
  - Incomplete parameterized type -> generic `<caret>` slot
  - Unclosed delimiter (`<` or `[`) -> close delimiter and construct block
- **Is it scalable across all types?**: Yes. All parameterized types (`Bytes<tsize>`, `Uint<tsize>`, `Vector<tsize, type>`, `Opaque<"tag">`) and composite types (`[T1, T2]`) follow the exact same closing and block attachment rules.

## Feature Implementation Map
- Processor: [`CompactSmartEnterProcessor.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/editor/smartEnter/CompactSmartEnterProcessor.java)
- Registration: [`plugin.xml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/resources/META-INF/plugin.xml) under `<lang.smartEnterProcessor>`
- Unit Tests: [`CompactSmartEnterTest.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/editor/CompactSmartEnterTest.java)

## Consequences & Future Maintenance
- When adding new language constructs (such as interfaces or union types if introduced in future Compact versions), add their pattern and block handler following this non-destructive pattern.
