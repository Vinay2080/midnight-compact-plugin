# ADR-005: Abstract Block Formatter & Indentation Model

## Status
Accepted (Phases 15–16 / Complete)

## Context & Problem Statement
Clean code style and predictable indentation are vital for smart contract readability, code reviews, and auditing.
In IntelliJ, code formatting and smart Enter indentation are driven by `FormattingModelBuilder` and recursive `Block` trees.
Without a dedicated formatting engine:
- Reformatting code (`Ctrl+Alt+L`) is a no-op or produces misaligned blocks.
- Pressing `Enter` after opening braces `{`, brackets `[`, or parameters `(` fails to indent child lines.
- Spacing around colons, commas, semicolons, and binary operators remains inconsistent.

## Authoritative References
1. **Compact Idiomatic Code Conventions**:
   - [`compact/compiler/standard-library.compact`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/compact/compiler/standard-library.compact): Official style for contract declarations, circuits, indentation (2 spaces), type colons, and spacing.
2. **Production JetBrains Implementations**:
   - `intellij-rust`: [`RsFormattingModelBuilder.kt`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/intellij-rust/src/main/kotlin/org/rust/ide/formatter/RsFormattingModelBuilder.kt) and `RsBlock.kt` — standard block tree implementation using `SpacingBuilder`.
   - `intellij-scala`: [`ScalaFormattingModelBuilder.scala`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/intellij-scala/scala/scala-impl/src/org/jetbrains/plugins/scala/lang/formatting/ScalaFormattingModelBuilder.scala).

## Decision
1. **Block-Based AST Formatting (`CompactBlock` extending `AbstractBlock`)**:
   - Decomposes the Compact AST into a hierarchy of `CompactBlock` instances, skipping raw whitespace tokens.
   - Computes child indentation dynamically in `computeChildIndent`:
     - Normal indent (2 spaces) inside statement blocks, struct bodies, enum variants, and circuit bodies.
     - Continuation indent for multi-line expressions and wrapped parameters.
     - None indent for top-level declarations and closing braces.
2. **Declarative Spacing Rules (`SpacingBuilder`)**:
   - Single space before `{` in declarations and control flow statements.
   - Space after `,` and `;`.
   - Space around binary and ternary operators (`+`, `-`, `*`, `?`, `:`).
   - No space between identifier and opening `(` or `<` in calls and type instantiations.
   - Space after type annotation colons (`:`).
3. **Smart Enter Indentation (`getChildAttributes`)**:
   When pressing `Enter` inside an unclosed block (`isIncomplete() == true`), `getChildAttributes` returns `Indent.getNormalIndent()`, perfectly placing the caret on the indented line.

## Scalability & Anti-Hardcoding Evaluation
- **Is it hardcoded?**: No. Rules are declared uniformly using `SpacingBuilder` token matching rules and AST element type hierarchies, applying consistently to all statements and expressions.
- **Does it preserve formatting stability?**: Yes. Idempotent formatting ensures running `Reformat Code` multiple times produces identical text.

## Feature Implementation Map
- Model Builder: [`CompactFormattingModelBuilder.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/formatter/CompactFormattingModelBuilder.java)
- Block Engine: [`CompactBlock.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/formatter/CompactBlock.java)
- Code Style Settings: [`CompactCodeStyleSettings.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/formatter/CompactCodeStyleSettings.java)
- Registration: [`plugin.xml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/resources/META-INF/plugin.xml) (`<lang.formatter>`)
- Test Suite: [`CompactFormatterTest.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/formatter/CompactFormatterTest.java)

## Consequences & Future Maintenance
- Formatting rules remain decoupled from parser logic. Custom user code style preferences can be added via `CompactCodeStyleSettingsProvider` without touching `CompactBlock`.
