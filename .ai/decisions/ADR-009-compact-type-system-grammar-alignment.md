# ADR-009: Strict Alignment with Upstream Compact Type Grammar

## Status
Accepted (Implemented in Phase 28 / v1.2.4)

## Context & Problem Statement
In languages like Java, Rust, or Solidity, byte slices or arrays are commonly denoted using square brackets (e.g. `byte[]` or `[u8; 32]`).
In Compact, however, byte types are **strictly parameterized with angle brackets** (`Bytes<32>`), while square brackets `[]` are reserved for tuple types (`[Field, Boolean]` or unit `[]`) or byte literal expressions (`Bytes[0x01, 0x02]`).
Recommending or completing `Bytes[]` in Compact was an erroneous assumption that violated the compiler's grammar. An authoritative architectural baseline across all Compact types was required.

## Authoritative References
1. **Official Compact Compiler Parser** ([`compact/compiler/parser.ss:624-656`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/compact/compiler/parser.ss#L624-L656)):
   ```scheme
   (Type (type)
     [type-ref :: tref => values]
     [type-boolean :: src (KEYWORD Boolean) => ...]
     [type-field :: src (KEYWORD Field) => ...]
     [type-unsigned-integer-bits :: src (KEYWORD Uint) #\< tsize #\> => ...]
     [type-unsigned-integer-max :: src (KEYWORD Uint) #\< tsize ".." tsize #\> => ...]
     [type-bytes :: src (KEYWORD Bytes) #\< tsize #\> => ...]
     [type-opaque :: src (KEYWORD Opaque) #\< str #\> => ...]
     [type-vector :: src (KEYWORD Vector) #\< tsize #\, type #\> => ...]
     [type-tuple :: src #\[ (SEP* type #\, #t) #\] => ...])
   ```
2. **Official Compact Compiler IR** ([`compact/compiler/langs.ss:290-300`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/compact/compiler/langs.ss#L290-L300)):
   - `(tboolean src)`
   - `(tfield src ftype)`
   - `(tunsigned src tsize)`
   - `(tunsigned src tsize tsize^)`
   - `(tbytes src tsize)`
   - `(topaque src opaque-type)`
   - `(tvector src tsize type)`
   - `(ttuple src type* ...)`
3. **Official Value Literal Syntax** ([`compact/compiler/parser.ss:864-869`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/compact/compiler/parser.ss#L864-L869)):
   - `Bytes[...]` is parsed strictly under `Expression (expr)` as `"bytes literal"`:
     ```scheme
     ["bytes literal" :: src (KEYWORD Bytes) #\[ (SEP* bytes-arg #\, #t) #\] => ...]
     ```
   - It is never a `Type`.

## Decision
1. **Strict Separation of Types vs Expressions**:
   - `Bytes<N>` is recognized as the only valid byte type syntax.
   - `Bytes[...]` is recognized strictly as an expression constructor literal.
   - `[...]` in type position is strictly recognized as a tuple type.
2. **Editor Completion Alignment**:
   - Typing `Byte` or `Bytes` in type context auto-completes to `Bytes<<caret>>`.
   - Typing `Uint` completes to `Uint<<caret>>`.
   - Typing `Vector` completes to `Vector<<caret>>`.
   - Partial angle bracket types (e.g. `Bytes<32`) close with `>` upon Smart Enter.
3. **Parser & Lexer Synchronization**:
   - Both our handwritten `CompactLexer` and `CompactParser` validate that `BYTES_TYPE` in type context requires `<` followed by a type size `tsize` and `>`.

## Scalability & Anti-Hardcoding Evaluation
- **Is it hardcoded?**: No. It directly mirrors the official Scheme compiler productions:
  - Primitives: `Boolean`, `Field`, `JubjubScalar`, `Secp256k1Base`, `Secp256k1Scalar`.
  - Angle-bracket parameterized: `Bytes<tsize>`, `Uint<tsize>`, `Uint<tsize..tsize>`, `Vector<tsize, type>`, `Opaque<"tag">`.
  - Bracketed: `[type* ...]`.
- **Is it scalable?**: Yes. Any new type introduced upstream in `parser.ss` can be added to the exact matching production in our parser and editor processors.

## Feature Implementation Map
- Parser: [`CompactParser.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/parser/CompactParser.java) (`parseBuiltinTypeBody`)
- Lexer: [`CompactLexer.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/lexer/CompactLexer.java)
- Smart Enter: [`CompactSmartEnterProcessor.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/editor/smartEnter/CompactSmartEnterProcessor.java)
- Type Inference: [`CompactTypeInferenceUtil.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/psi/util/CompactTypeInferenceUtil.java)
- Unit Tests: [`CompactSmartEnterTest.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/editor/CompactSmartEnterTest.java)

## Consequences & Future Maintenance
- Never introduce array syntax `[]` for `Bytes` in type contexts. If array types are ever added to Compact in future language versions, inspect `compact/compiler/parser.ss` first to determine the exact upstream syntax.
