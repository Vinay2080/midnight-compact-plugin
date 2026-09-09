# ADR-024: Two-Tier Semantic Syntax Highlighting & Color Settings Page

## Status
Accepted (Phases 3–4 / Complete)

## Context & Problem Statement
In smart contract programming, visual distinctions are essential for safety and auditability:
1. Lexical highlighting (based solely on token types like keywords and numbers) cannot distinguish between a read of a ledger state variable and a read of a local variable, nor can it distinguish an on-chain `circuit` call from an off-chain `witness` call.
2. Ledger state mutations (`ledger.counter = counter + 1;`) must visually stand out from pure local variable reassignments.
3. Developers use a wide variety of IDE themes (Light, Darcula, IntelliJ Dark, High Contrast) and require customizable color settings.

## Authoritative References
1. **IntelliJ Highlighting Architecture**:
   - Tier 1: `SyntaxHighlighter` / `SyntaxHighlighterFactory` (Lexical highlighting based on `LexerBase` token sets).
   - Tier 2: `Annotator` (`com.intellij.lang.annotation.Annotator`) (Semantic highlighting based on resolved AST nodes).
   - `ColorSettingsPage` for theme customization.
2. **Production JetBrains Implementations**:
   - `intellij-rust`: `RsHighlighter.kt`, `RsHighlightingAnnotator.kt`, `RsColorSettingsPage.kt`.

## Decision
1. **Two-Tier Highlighting Pipeline**:
   - **Tier 1 (Lexical)**: `CompactSyntaxHighlighter` assigns base colors (keywords, strings, numeric literals, comments, operators, delimiters) instantly during lexing.
   - **Tier 2 (Semantic)**: `CompactHighlightingAnnotator` runs on the parsed PSI tree, analyzing resolved symbols to assign rich domain-specific `TextAttributesKey` values:
     - `CIRCUIT_DECLARATION` vs `WITNESS_DECLARATION`
     - `CIRCUIT_CALL` vs `WITNESS_CALL`
     - `LEDGER_USAGE` (read) vs `LEDGER_WRITE` (mutation)
     - `LOCAL_VARIABLE_USAGE` vs `LOCAL_VARIABLE_WRITE`
     - `BUILTIN_FUNCTION` (standard library hash functions like `transient_hash`)
2. **Dedicated Color Settings Page (`CompactColorSettingsPage`)**:
   - Exposes every semantic token category under **Settings -> Editor -> Color Scheme -> Compact**.
   - Provides a comprehensive demo code snippet showcasing all declaration, call, and mutation highlights.

## Scalability & Anti-Hardcoding Evaluation
- **Is it hardcoded?**: No. Semantic highlighting queries resolved PSI elements (`resolve() instanceof CompactCircuitDefinition`) rather than matching identifier names.
- **Is it fast?**: Yes. Base lexical coloring is immediate, and semantic annotations are applied incrementally by IntelliJ's background daemon.

## Feature Implementation Map
- Lexical Highlighter: [`CompactSyntaxHighlighter.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/highlighter/CompactSyntaxHighlighter.java)
- Semantic Annotator: [`CompactHighlightingAnnotator.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/highlighter/CompactHighlightingAnnotator.java)
- Color Attributes: [`CompactHighlighterColors.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/highlighter/CompactHighlighterColors.java)
- Color Settings Page: [`CompactColorSettingsPage.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/highlighter/CompactColorSettingsPage.java)
- Registration: [`plugin.xml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/resources/META-INF/plugin.xml) (`<lang.syntaxHighlighterFactory>`, `<annotator>`, `<colorSettingsPage>`)
- Unit Tests: [`CompactHighlightingAnnotatorTest.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/highlighter/CompactHighlightingAnnotatorTest.java)

## Consequences & Future Maintenance
- Any new semantic construct or keyword should be registered in `CompactHighlighterColors` and mapped in `CompactColorSettingsPage`.
