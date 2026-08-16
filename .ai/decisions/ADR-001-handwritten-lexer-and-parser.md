# ADR-001: Handwritten Lexer and Recursive-Descent Parser

## Context
The Midnight Compact language contains distinctive syntax elements (e.g. `disclose`, `ledger`, `circuit`, `witness`, `as`, `Vector<T, N>`, pragma versions) and complex operator precedence. In IntelliJ IDEA, plugins can use either generated parsers (GrammarKit/JFlex) or handwritten lexers and parsers implementing IntelliJ core interfaces (`LexerBase`, `PsiParser`).

## Decision
Implement a handwritten lexer (`CompactLexer` extending `LexerBase`) and a handwritten recursive-descent parser (`CompactParser` implementing `PsiParser`).

## Alternatives Considered
1. **GrammarKit + JFlex generation**: Generating parser and lexer from `.bnf` and `.flex` files.
2. **Chez Scheme AST Transpilation**: Running the upstream compiler out-of-process to produce ASTs.

## Why
- Complete control over error recovery and resilient parsing during active typing.
- Elimination of external build-time code generation dependencies.
- Zero-latency parsing directly inside IntelliJ EDT/background threads.
- Precise loop advancement guards to prevent IDE freezes on malformed syntax.

## Consequences
- Lexer and parser modifications must be maintained in Java source code.
- Grammar specifications (`references/compact-grammar.mdx` and `compact/compiler/parser.ss`) serve as semantic references rather than build artifacts.
