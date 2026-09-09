# ADR-001: Handwritten Lexer and Recursive-Descent Parser Engine

## Status
Accepted (Foundational / Phase 1–3)

## Context & Problem Statement
Compact is a domain-specific smart contract language for the Midnight privacy blockchain. It combines TypeScript-like declaration syntax with zero-knowledge primitives (`disclose`, `witness`, `circuit`, `pure circuit`, `sealed`, `ledger`), fixed-size numeric types (`Field`, `Bytes<N>`, `Uint<N>`), ledger state cells (`Cell<T>`, `Map<K, V>`), and custom operators (`..`, `++`, `->`).

JetBrains plugins typically choose between two architectural approaches for syntax analysis:
1. **Generated Lexers/Parsers (GrammarKit + JFlex)**: Generate Java source files from `.flex` and `.bnf` grammar descriptions during Gradle build.
2. **Handwritten Recursive-Descent Lexer & Parser**: Direct implementation of IntelliJ's `LexerBase` and `PsiParser` interfaces in Java.

Generated parsers often struggle with:
- Error recovery during incomplete, live keystroke typing.
- Ambiguous lookahead disambiguation (e.g. angle brackets `<` for generics vs relational comparison; `Bytes[...]` expression literals vs `Bytes<N>` types).
- Build-time code generation overhead and Gradle cache friction.
- Resilient recovery from missing semicolons or malformed contract bodies without dropping following declarations.

## Authoritative References
1. **Official Midnight Compiler Grammar & Lexer**:
   - [`compact/compiler/lexer.ss`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/compact/compiler/lexer.ss): Lexical rules for identifiers, comments (`seen-slash`, `lex-block-comment`), numbers (hex, binary, octal, decimal), and version strings.
   - [`compact/compiler/parser.ss`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/compact/compiler/parser.ss): Full EBNF production rules for programs, declarations, statements, expressions, and precedence table.
2. **Production JetBrains Implementations**:
   - `intellij-elixir`: [`ElixirLexer.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/intellij-elixir/src/org/elixir_lang/lexer/ElixirLexer.java) and [`ElixirParser.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/intellij-elixir/src/org/elixir_lang/parser/ElixirParser.java) — standard-setting production handwritten lexer and parser using `LexerBase` with token-lookahead error recovery.

## Decision
1. **Handwritten Lexer (`CompactLexer`)**:
   - Extends `LexerBase` directly, implementing high-throughput, allocation-free scanning across `CharSequence` buffers.
   - Disambiguates keywords and built-in types using static immutable `Map.ofEntries` lookups (`CompactTokenTypes.KEYWORDS`, `BUILTIN_TYPES`).
   - Rejects nested block comments cleanly to mirror compiler `(nested-comment-error)` without throwing exceptions.
2. **Handwritten Recursive-Descent Parser (`CompactParser`)**:
   - Implements `PsiParser` using IntelliJ's `PsiBuilder`.
   - Structures parsing into discrete, isolated rule functions: `parseProgram`, `parseDeclaration`, `parseContractBody`, `parseStatement`, `parseExpression`.
   - Employs **Strict Loop Advancement Guards**: every `while` loop checks `builder.getTokenType()` and enforces `builder.advanceLexer()` or marks `builder.error()` to mathematically prevent infinite loop UI freezes during typing.
   - Provides **Local Error Boundaries**: a syntax error inside a circuit or statement only emits a local `PsiErrorElement` and syncs on the next semicolon `;` or closing brace `}`, keeping all subsequent declarations fully navigable in the PSI tree.

## Scalability & Anti-Hardcoding Evaluation
- **Is it hardcoded?**: No. The parser directly mirrors the official Scheme EBNF grammar hierarchy from `compact/compiler/parser.ss`. Operators and precedence follow a standardized Pratt/precedence-climbing ladder rather than arbitrary `if-else` chains.
- **Does it recover from incomplete code?**: Yes. Every construct checks for end-of-file (`builder.eof()`) and synchronization tokens (`RBRACE`, `SEMI`, declaration keywords), ensuring the IDE remains responsive and error markers appear exactly at the offending token.

## Feature Implementation Map
- Lexer: [`CompactLexer.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/lexer/CompactLexer.java)
- Tokens: [`CompactTokenTypes.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/psi/CompactTokenTypes.java)
- Parser: [`CompactParser.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/parser/CompactParser.java)
- Element Types: [`CompactElementTypes.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/psi/CompactElementTypes.java)
- Parser Definition: [`CompactParserDefinition.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/parser/CompactParserDefinition.java)
- Registration: [`plugin.xml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/resources/META-INF/plugin.xml) (`<lang.parserDefinition>`)
- Test Suites:
  - [`CompactLexerTest.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/lexer/CompactLexerTest.java)
  - [`CompactParserTest.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/parser/CompactParserTest.java)

## Consequences & Future Maintenance
- Any new keywords, statements, or operators added to the upstream Midnight compiler must be added directly to `CompactTokenTypes`, `CompactLexer`, and `CompactParser`.
- Parser tests must uphold the 5-Tier Testing Strategy (Golden Tree, Partial Parsing, Precedence, Stress/Nesting, Concurrency).
