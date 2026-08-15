# Project Goal

Build an IntelliJ IDEA plugin for the Midnight Compact language used for Midnight smart contracts.

Core technologies and feature areas:

- IntelliJ Platform plugin
- Compact language support
- Handwritten lexer extending `LexerBase`
- Handwritten recursive-descent parser implementing `PsiParser`
- Handwritten PSI and AST element types
- ParserDefinition
- Syntax highlighting
- References
- Completion
- Inspections
- Formatter

## Progress

- [x] Lexer
- [x] Syntax Highlighting
- [x] Reference BNF reviewed
- [x] Parser
- [x] Handwritten PSI wrappers
- [x] ParserDefinition
- [x] Lexer/parser regression tests
- [x] References
- [x] Completion
- [x] Rename
- [x] Find Usages
- [x] Phase 3 & 4 Automated Test Suite (66/66 unit tests passing across resolution, references, completion, rename, find usages, and type inference)
- [x] Phase 1 Extended Test Suite (74/74 unit tests passing including lexer edge cases, string escapes, numeric boundaries, reserved keywords, advanced control flows, and AST error recovery)
- [x] Phase 2 Extended Test Suite (80/80 unit tests passing including parameter shadowing, destructured pattern resolution, custom type alias resolution, soft-unresolved references, generic parameters, and struct field references)
- [x] Phase 3 Extended Test Suite (85/85 unit tests passing including generic type parameter completion, enum/enum-member renaming, struct field & enum member find usages, and validator invalid identifier rejection)
- [x] Phase 4 Extended Test Suite (91/91 unit tests passing including cast expression typing, string literal typing, nested arithmetic & logical binary typing, struct literal type inference, and unknown reference type fallbacks)
- [x] Parser Freeze & EDT Deadlock Fix (Fixed infinite parsing loop on isolated `export` keywords/invalid modifier sequences during background document commit; added regression test suite, 95/95 unit tests passing)
- [x] Type Inference
- [x] Updated README documentation (Problem statement, implemented features, roadmap, build instructions)
- [x] Phase 5 Semantic Inspections & QuickFix Test Suite (138/138 unit tests passing across resolution, references, completion, rename, find usages, type inference, unresolved reference inspection, duplicate declaration inspection, unused local variable inspection with quick-fix, and type mismatch inspection)
- [x] Inspections
- [x] Formatter
- [x] Phase 6 Formatter & Smart Indentation Test Suite (177/177 unit tests passing across basic formatting, nested blocks, declarations, expressions, comments, malformed/incomplete code resilience, idempotence, and official contract formatting regressions)

## Compiler Files Reviewed

| Compiler File                   | Importance | Grammar Changes Needed | Semantic Features Discovered          | Plugin Impact                             |
|---------------------------------|------------|------------------------|---------------------------------------|-------------------------------------------|
| Compact language grammar source | High       | Yes                    | Syntax model for Compact programs     | Lexer, BNF grammar, parser, generated PSI |
| lexer.l                         | High       | Yes                    | Tokenization rules for Compact syntax | JFlex lexer and token types               |

## Missing

- None (all core feature areas implemented: Lexer, Parser, PSI, Highlighter, References, Completion, Rename, Find Usages, Type Inference, Inspections, Formatter)

## Decisions

- Parser follows the official Compact language grammar where available.
- Compiler optimizations are ignored unless they affect IDE-visible semantics.
- `grammar/reference.bnf` and `grammar/compact.flex` / `src/main/grammar/*` are reference material only for the current
  handwritten implementation; do not regenerate parser or lexer code from them unless the project explicitly migrates.
- `src/main/gen` currently contains handwritten source despite the directory name. Treat it as editable project source.
- References should be implemented using IntelliJ `PsiReference` APIs.
- `AGENTS.md` is the permanent engineering notebook and must be updated after every completed task that changes project
  knowledge or status.

### Current Architecture

```text
CompactLexer
↓
CompactTokenTypes
↓
CompactParser
↓
CompactElementTypes
↓
CompactPsiElement
↓
CompactParserDefinition
↓
CompactFileType
↓
CompactSyntaxHighlighter
↓
References & Completion
↓
Semantic Inspections & Quick-Fixes
↓
Formatter & Smart Indentation
```

## Phase 3 Architecture — References, Resolution & Completion

- Phase 3 is implemented as a PSI/reference layer on top of the handwritten parser and existing element types; `CompactParser.java`, token types, and grammar reference files remain unchanged.
- References are owned by handwritten PSI wrappers rather than a central `PsiReferenceContributor`: value references on `REFERENCE_EXPR`, type references on `TYPE_REFERENCE` and struct literals, enum-member references on `MEMBER_EXPR`, import references on import forms/elements, and implements targets through the contained type reference.
- Resolution uses single-file `PsiTreeUtil` scope walking in `CompactResolveUtil` with separate value/type namespaces and innermost-first shadowing; it intentionally avoids stubs, indexes, cross-file include/import resolution, stdlib symbol indexes, and type inference.
- Named wrappers now cover parameters, struct fields, enum members, const bindings, generic parameters, simple patterns, and import aliases so Go To Declaration, Rename, and Find Usages share the same `CompactNamedElement` model.
- Module imports are modeled in-file: `import M prefix $;` exposes exported module members as flat `$name` references/completions, and `import { a as b } from M;` treats the alias as the local named declaration while the source name references the module export.
- Completion is contextual through `CompactCompletionContributor`: declaration/statement keywords at starts, builtin and in-scope types in type positions, in-scope values plus value keywords in expression positions, and enum members after `Enum.`; struct field and ADT method completion remain deferred until type inference.
- Find Usages uses `DefaultWordsScanner` over the handwritten lexer and `CompactNamedElement` classification; Rename uses `CompactNamedElementImpl.setName`, reference `handleElementRename`, and `CompactNamesValidator` to reject keywords and invalid identifiers.
- Open questions remain: exact disjointness of type/value namespaces for same-name declarations, visibility of non-exported module members, cross-file include/import semantics, stdlib builtin resolution surface, and first-class destructuring/for-loop binding ownership without parser changes.

## Phase 4 Architecture — Type Inference

- Phase 4 implements a lightweight type system on top of the PSI, enabling `getType()` for expressions and named elements.
- Core types are modeled in Java/Kotlin (`CompactType`, `CompactPrimitiveType`) representing Boolean, Field, Uint, and nominal types for structs/enums.
- All expression PSI nodes (Reference, Call, Member, Literal, Binary, Unary) implement a common `CompactExpression` interface.
- All named declarations (Parameter, Const, Struct, Enum) implement `CompactTypeElement` to provide their declared or inferred types.
- Type inference handles:
    - Literals (Boolean, Numeric).
    - Reference resolution to typed declarations.
    - Binary and Unary operators via `CompactTypeInferenceUtil`.
    - Struct field access via `CompactStructFieldReference`, which uses the inferred type of the base expression to resolve the field in the target struct definition.
- The implementation avoids complex dataflow analysis or global type checking, focusing on local, editor-visible type information.

## Phase 5 Architecture — Semantic Inspections & Quick Fixes

- Phase 5 implements a first-class IntelliJ inspection layer via `LocalInspectionTool` subclasses registered in `plugin.xml`:
    - **`CompactUnresolvedReferenceInspection`**: Flags unresolved identifier references (`CompactReferenceExprImpl`), enum member accesses (`CompactEnumMemberReference`), and struct field accesses (`CompactStructFieldReference`). Soft-unresolved type references (such as builtins, stdlib, or external include types) are intentionally not flagged to prevent false positives in single-file analysis.
    - **`CompactDuplicateDeclarationInspection`**: Detects duplicate sibling declarations within the same scope container (file, module, callable parameters, struct fields, enum members, local blocks). Distinguishes `TYPE` vs `VALUE` namespaces and correctly handles nested block shadowing without false positives.
    - **`CompactUnusedLocalVariableInspection`**: Identifies unused local variable bindings within callable bodies (`circuit`, `witness`, `constructor`) using `ReferencesSearch`. Excludes top-level constants, parameters, struct fields, and underscore-prefixed bindings (`_name`). Provides the `CompactRemoveUnusedVariableFix` quick-fix to safely delete unused variable statements.
    - **`CompactTypeMismatchInspection`**: Verifies type safety using the Phase 4 lightweight type system: requires `Boolean` operands for logical (`&&`, `||`) and negation (`!`) expressions, and checks type compatibility for equality operators (`==`, `!=`). Defers inspection whenever operand types evaluate to `CompactPrimitiveType.UNKNOWN` to prevent cascading false positives on incomplete or un-inferable code.
- All inspections include resilience guards that safely ignore `PsiErrorElement` trees and incomplete PSI structures during interactive editing.

## Phase 6 Architecture — Formatter & Smart Indentation

- Phase 6 implements native IntelliJ formatting (`Ctrl + Alt + L`) and smart indentation support for Compact source files:
    - **`CompactFormattingModelBuilder`**: Registered via `<lang.formatter>` in `plugin.xml`, creates a `DocumentBasedFormattingModel` wrapping a hierarchical `CompactBlock` AST block tree.
    - **`CompactBlock`**: Extends `AbstractBlock`, computes node-level indentation, child attributes for smart Enter handling (`getChildAttributes`), and context-aware spacing (`getSpacing`) delegating to `SpacingBuilder`.
    - **`CompactLanguageCodeStyleSettingsProvider`**: Registered via `<langCodeStyleSettingsProvider>` in `plugin.xml`, configures default 2-space indentation (`INDENT_SIZE = 2`, `TAB_SIZE = 2`, `CONTINUATION_INDENT_SIZE = 2`, `USE_TAB_CHARACTER = false`) matching canonical Compact style.
    - **Formatting Rules & Spacing**:
        - Binary operators (`+`, `-`, `*`, `/`, `%`, `==`, `!=`, `<=`, `>=`, `&&`, `||`, `=>`, `?`, `as`) have 1 space around them.
        - Delimiters (`,`, `;`, `:`) have 0 spaces before and 1 space after (with `:` in return types having 0 spaces before, 1 after).
        - Member access (`.`), range (`..`), and spread (`...`) have 0 spaces around them.
        - Generics angle brackets (`< ... >`) have 0 spaces inside and 0 spaces before in type/call contexts.
        - Control statements (`if`, `for`) have 1 space before `(`, callable invocations and signatures have 0 spaces before `(`.
        - Struct literals (`mariusz { 1, 2, 3, }`) and export forms (`export { Maybe };`) have 1 space inside braces.
    - **Indentation**:
        - 2-space normal indent for children of `BLOCK`, `STRUCT_DECLARATION`, `ENUM_DECLARATION`, `CONTRACT_DECLARATION`, and `MODULE_DEFINITION`.
        - Enclosing delimiters (`{`, `}`, `(`, `)`, `[`, `]`) maintain parent indentation.
        - Statement bodies in `if` / `for` without braces receive normal indent.
    - **Resilience & Error Recovery**:
        - Ignores and handles `PsiErrorElement` nodes gracefully without crashing.
        - Idempotent formatting: `format(format(x)) == format(x)`.

## Test Infrastructure & Context Reference

To streamline future test development and lower API credit consumption, key test patterns and element type mappings are recorded here:

### 1. Test Framework Setup & Classes
- **Lexer Tests** (`LexerTest.java`): Standalone JUnit 4 test class using `CompactLexer.start(text, 0, len, 0)` and `assertTokens(text, IElementType...)`.
- **Parser & Recovery Tests** (`ParsingTestCase` subclasses): `DeclarationParserTest`, `StatementParserTest`, `ExpressionParserTest`, `ErrorRecoveryParserTest`. Extends IntelliJ `ParsingTestCase("category", "compact", new CompactParserDefinition())` with `parseFile("Name", text)` and `DebugUtil.psiToString(file, true)`.
- **Fixture Tests** (`BasePlatformTestCase` subclasses): `CompactResolveTest`, `CompactReferenceTest`, `CompactCompletionTest`, `CompactRenameTest`, `CompactFindUsagesTest`, `CompactTypeInferenceTest`. Configured using `myFixture.configureByText(CompactFileType.INSTANCE, text)`.

### 2. Token & AST Element Mappings Quick Reference
- **Equality Operator**: `CompactTokenTypes.EQEQ` (`==`), `NEQ` (`!=`), `ASSIGN` (`=`).
- **Control & Keywords**: `CompactTokenTypes.LET` (`let`), `CONST` (`const`), `DISCLOSE` (`disclose`), `FOR` (`for`), `IF` (`if`), `STRUCT` (`struct`), `ENUM` (`enum`).
- **AST Nodes**: `DISCLOSE_EXPR`, `PATTERN` (destructuring patterns), `TUPLE_EXPR`, `STRUCT_DECLARATION`, `CIRCUIT_DEFINITION`, `CONST_STATEMENT`, `RETURN_STATEMENT`, `PsiErrorElement`.

## Compact Language Reference

The official Compact compiler repository located at `./compact` contains the authoritative specification, lexer, parser, AST schemas, and test fixtures for the Compact language.

### Important Official Files

#### Grammar
- `compact/compiler/parser.ss` (Authoritative Source): Executable Compact grammar definition in Chez Scheme using `define-grammar` macros. Defines top-level parsing rules, operator precedence hierarchy (`expr` through `expr9`/`term`), declaration syntax, statement structures, and keyword lookup tables.
- `compact/doc/compact-grammar.mdx` (Generated Code / Reference): Rendered formal EBNF grammar specification generated by the compiler's documentation tool.
- `compact/compiler/compact-reference-proto.mdx` (Reference): Markdown template prototype containing `@(request-snippet)` macro tags for embedding grammar rules.
- `compact/editor-support/vsc/compact/syntaxes/compact.tmLanguage.json` (Reference): VS Code TextMate grammar JSON for syntax highlighting.
- `compact/specification/src/Syntax/` (Reference): Formal Agda specification of Compact syntax.

#### Tokens / Lexer
- `compact/compiler/lexer.ss` (Authoritative Source): Handwritten Chez Scheme lexer implementing state-machine tokenization (`define-state-case`). Converts raw character input into a lazy stream of `$make-token` records containing source spans, token categories (`'id`, `'field`, `'string`, `'version`, `'binop`, `'punctuation`, `'line-comment`, `'block-comment`, `'whitespace`, `'eof`), and unescaped values.
- `compact/compiler/export-keywords.ss` (Supporting Implementation): Utility module exporting parser keyword lists for downstream compiler passes.

#### Parser Engine & Framework
- `compact/compiler/parser.ss` (Authoritative Source): Entry-point functions (`parse-file`, `parse-file/token-stream`, `parse`).
- `compact/third_party/compiler/ez-grammar.ss` (Supporting Implementation): Monadic parser combinator framework adapted from Chez Scheme (`define-grammar`, `is`, `sat`, `sat/what`, `item`, `peek`, `seq`, `++`, `+++`, `many`, `many+`, `?`). Implements left-recursion elimination, monadic backtracking, source range wrapping, and error reporting.

#### AST (Abstract Syntax Tree)
- `compact/compiler/lparser.ss` (Authoritative Source / AST): `Lparser` Nanopass CST language schema. Represents the concrete parse tree including punctuation tokens, keyword tokens, separators, and source location objects.
- `compact/compiler/lparser-to-lsrc.ss` (Supporting Implementation): `Lparser->Lsrc` Nanopass lowering pass. Strips concrete syntax nodes (commas, semicolons, brackets, keywords) and transforms tokens into Scheme symbols, numbers, and bytevectors.
- `compact/compiler/langs.ss` (Authoritative Source / AST): `Lsrc` surface AST schema (canonical AST representation of Compact source programs) and 25+ downstream intermediate compiler pass schemas (`Lnoinclude`, `Lsingleconst`, `Lnopattern`, `Lhoisted`, `Lexpr`, `Ltypes`, `Lcircuit`, `Lzkir`, etc.).

#### Error Handling
- `compact/compiler/parser.ss` (Authoritative Source): Failure formatting functions (`format-failure`, `format-input-token`) returning human-readable parse error messages ("parse error: found X looking for Y").
- `compact/compiler/sourcemaps.ss` & `compact/compiler/lexer.ss` (Supporting Implementation): Source location tracking (`make-source-object`) encapsulating line numbers, column numbers, and zero-based beginning/ending file character offsets (`bfp`, `efp`).

#### Parser Tests
- `compact/compiler/test.ss` (Test / Reference): ~3.8 MB test suite containing lexer tests, parser unit tests, round-trip formatter/reparse tests (`parse-file/format/reparse`), and error diagnostic assertion tests.
- `compact/test-contracts/` (Test / Reference): E2E integration test contract fixtures and test orchestrator (`compact-test-orchestrator.test.ts`, `compact-test-runner.mjs`).

#### Syntax Examples
- `compact/examples/*.compact` (Test / Reference): Representative valid Compact contracts (`counter.compact`, `election.compact`, `proposal.compact`, `tiny.compact`, `zerocash.compact`).
- `compact/examples/errors/*.compact` (Test / Reference): 15 focused invalid/malformed test contracts designed to test compiler error diagnostics (`counter_direct_assignment.compact`, `duplicate_ledger_binding.compact`, `invalid_cast_to_map_type.compact`, `missing-include.compact`, `sealed_ledger_mutation.compact`, `unbound.compact`, etc.).

#### Supporting Files
- `compact/compiler/standard-library.compact` (Supporting Implementation / Reference): Built-in Compact standard library contract source parsed at runtime.
- `compact/compiler/zkir-v3-library.compact` (Supporting Implementation / Reference): Built-in ZKIR v3 library definitions.

### Parsing Pipeline Architecture

```text
Compact Source Text (.compact)
          │
          ▼
[compact/compiler/lexer.ss]
  - State machine character scanning (`define-state-case`)
  - Generates lazy stream of `$make-token` records (with source spans `bfp..efp`)
          │
          ▼
Stream Filtering
  - `stream-filter` strips `'whitespace`, `'line-comment`, and `'block-comment` tokens
          │
          ▼
[compact/compiler/parser.ss] via [compact/third_party/compiler/ez-grammar.ss]
  - Monadic parser combinators (`define-grammar`) with ordered choice backtracking (`++`)
  - Keywords matched via symbol hashtable (`keyword?`)
          │
          ▼
Lparser AST (`compact/compiler/lparser.ss`)
  - Concrete Syntax Tree (CST) retaining keyword tokens, punctuation, brackets, separators
          │
          ▼
[compact/compiler/lparser-to-lsrc.ss]
  - Nanopass AST conversion (`Lparser->Lsrc`)
  - Strips concrete syntax tokens; unbinds token values to raw symbols, field numbers, bytevectors
          │
          ▼
Lsrc AST (`compact/compiler/langs.ss`)
  - Canonical Surface Abstract Syntax Tree
          │
          ▼
Compiler Pass Pipeline (Frontend -> Analysis -> Lowering -> ZKIR Code Gen)
```

## IntelliJ Parser Porting Notes

### 1. Token Mappings (`CompactTokenTypes` vs Official Tokens)
- **Official Token Stream**: The official lexer (`lexer.ss`) produces coarse token categories (`id`, `field`, `string`, `version`, `binop`, `punctuation`, `line-comment`, `block-comment`, `whitespace`, `eof`). Keyword identification is deferred to `parser.ss` via `keyword?` lookup tables.
- **IntelliJ Lexer (`CompactLexer.java`)**: Idiomatically categorizes keywords and primitive types directly into explicit `IElementType` instances during lexing.
- **Keyword Alignment**:
  - Declarations & Control: `pragma`, `export`, `from`, `import`, `module`, `prefix`, `assert`, `as`, `circuit`, `const`, `constructor`, `contract`, `default`, `disclose`, `else`, `enum`, `fold`, `for`, `if`, `implements`, `include`, `ledger`, `map`, `new`, `of`, `pad`, `pure`, `return`, `sealed`, `slice`, `struct`, `type`, `witness`, `emit`, `external`. (All mapped in `CompactTokenTypes.java`).
  - Primitive Types: `Boolean`, `Bytes`, `Field`, `Opaque`, `Uint`, `Vector`. (`CompactTokenTypes.java` maps these plus stdlib aliases `JubjubScalar`, `Secp256k1Base`, `Secp256k1Scalar`).
  - Reserved Keywords: Official compiler reserves 36 future keywords in `keywordReservedForFutureUse` (`arguments`, `await`, `break`, `case`, `catch`, `class`, `continue`, `debugger`, `delete`, `do`, `eval`, `event`, `extends`, `finally`, `function`, `in`, `instanceof`, `interface`, `package`, `private`, `protected`, `public`, `static`, `super`, `switch`, `this`, `throw`, `try`, `typeof`, `var`, `void`, `while`, `with`, `yield`). Currently missing from `CompactTokenTypes.java` except `let` (line 150).

### 2. Lexer Capabilities vs Missing Constructs
- **Currently Supported by Plugin Lexer**: Hex (`0x`), Binary (`0b`), Octal (`0o`), Decimal, Single/Double quotes, line/block comments, versions (`1.2.3`), operators (`+`, `-`, `*`, `/`, `==`, `!=`, `<=`, `>=`, `&&`, `||`, `=>`, `..`, `...`), delimiters, and all active keywords.
- **Missing Tokens / Lexer Fixes Required**:
  - Add missing reserved keywords (`arguments`, `await`, `break`, `case`, `catch`, `class`, `continue`, `debugger`, `delete`, `do`, `eval`, `event`, `extends`, `finally`, `function`, `in`, `instanceof`, `interface`, `package`, `private`, `protected`, `public`, `static`, `super`, `switch`, `this`, `throw`, `try`, `typeof`, `var`, `void`, `while`, `with`, `yield`) to `KEYWORDS` map in `CompactLexer.java` and `CompactTokenTypes.java` so reserved keyword parsing and highlighting function as expected by the official compiler.

### 3. PsiBuilder Adaptation Strategies
- **Monadic Combinators -> PsiBuilder Markers**:
  - `ez-grammar.ss` monadic choice (`++`) maps directly to `PsiBuilder.Marker` rollback:
    ```text
    PsiBuilder.Marker m = builder.mark();
    if (parseBranch(builder)) {
        m.done(ELEMENT_TYPE);
    } else {
        m.rollbackTo();
    }
    ```
- **Expression Parsing**:
  - Official parser uses a 12-level precedence hierarchy (`expr` -> `expr0` -> ... -> `expr9` -> `term`).
  - `CompactParser.java` should implement Pratt parsing / precedence climbing for expressions to avoid deep recursive stack frames and maintain exact operator precedence.

### 4. AST Node -> PSI Element Mapping
Official `Lparser` / `Lsrc` nodes map to IntelliJ PSI element types as follows:

| Upstream AST Node (`lparser.ss` / `langs.ss`) | Proposed IntelliJ `IElementType` | PSI Class / Interface |
| :--- | :--- | :--- |
| `Program` | `CompactFile` (File Node) | `CompactFile` |
| `Pragma` (`pdecl`) | `PRAGMA_FORM` | `CompactPragmaForm` |
| `Include` (`incld`) | `INCLUDE_DECLARATION` | `CompactIncludeDeclaration` |
| `Module-Definition` (`mdefn`) | `MODULE_DEFINITION` | `CompactModuleDefinition` |
| `Import-Declaration` (`idecl`) | `IMPORT_DECLARATION` | `CompactImportDeclaration` |
| `Export-Declaration` (`xdecl`) | `EXPORT_DECLARATION` | `CompactExportDeclaration` |
| `Ledger-Declaration` (`ldecl`) | `LEDGER_DECLARATION` | `CompactLedgerDeclaration` |
| `Ledger-Constructor` (`lconstructor`) | `CONSTRUCTOR_DECLARATION` | `CompactConstructorDeclaration` |
| `Circuit-Definition` (`cdefn`) | `CIRCUIT_DEFINITION` | `CompactCircuitDefinition` |
| `Witness-Declaration` (`wdecl`) | `WITNESS_DECLARATION` | `CompactWitnessDeclaration` |
| `Contract-Implements-Declaration` (`cidecl`) | `CONTRACT_IMPLEMENTS_DECLARATION` | `CompactContractImplementsDeclaration` |
| `External-Contract-Declaration` (`ecdecl`) | `EXTERNAL_CONTRACT_DECLARATION` | `CompactExternalContractDeclaration` |
| `Structure-Definition` (`structdef`) | `STRUCT_DEFINITION` | `CompactStructDefinition` |
| `Enum-Definition` (`enumdef`) | `ENUM_DEFINITION` | `CompactEnumDefinition` |
| `Type-Definition` (`tdefn`) | `TYPE_DEFINITION` | `CompactTypeDefinition` |
| `Block` (`blck`) | `BLOCK_STATEMENT` | `CompactBlockStatement` |
| `Statement` (`stmt`) | `IF_STATEMENT`, `FOR_STATEMENT`, `CONST_STATEMENT`, `RETURN_STATEMENT`, `EXPR_STATEMENT` | `CompactStatement` |
| `Expression` (`expr`) | `BINARY_EXPR`, `UNARY_EXPR`, `CALL_EXPR`, `INDEX_EXPR`, `MEMBER_EXPR`, `TUPLE_EXPR`, `NEW_EXPR`, `CAST_EXPR`, `TERNARY_EXPR`, `PAREN_EXPR`, `LITERAL_EXPR` | `CompactExpression` |
| `Type` (`type`) | `TYPE_REF`, `BUILTIN_TYPE`, `TUPLE_TYPE` | `CompactType` |

### 5. Error Recovery Strategies
- **Top-level / Program Elements**: Recover at `;` or key declaration keywords (`pragma`, `import`, `export`, `contract`, `circuit`, `struct`, `enum`, `type`, `ledger`, `module`, `include`, `witness`).
- **Statement Level**: Recover at `;` or `}`.
- **Struct / Enum / Module Bodies**: Recover at `;` or `}` to prevent a single line syntax error from corrupting the surrounding AST container node.

### 6. Precedence & Associativity Rules
- **Ternary `? :` & Assignments (`=`, `+=`, `-=`)**: Right-associative (lowest expression precedence).
- **Binary Logical (`||`, `&&`)**: Left-associative.
- **Equality (`==`, `!=`) & Relational (`<`, `<=`, `>`, `>=`)**: Non-associative / left-associative.
- **Type Cast (`expr as Type`)**: Left-associative.
- **Additive (`+`, `-`) & Multiplicative (`*`, `/`)**: Left-associative.
- **Unary (`!`)**: Right-associative prefix.
- **Postfix Member/Call/Index (`.field`, `.method()`, `[idx]`)**: Left-associative postfix (highest expression precedence).

### 7. Ambiguities & Open Questions
- **Generic Arguments vs Relational Operators (`<` and `>`)**:
  - Disambiguating `Foo<Bar>` vs `a < b > c`.
  - In `ez-grammar.ss`, generic argument lists `<...>` backtrack when parsing type references or imports.
  - In IntelliJ `PsiBuilder`, parsing `<` in type contexts must look ahead for valid type identifiers/arguments before committing to `GENERIC_ARGUMENT_LIST`, or rollback to relational `<` operator.

## IntelliJ Plugin Architecture References

### IntelliJ Rust

The `intellij-rust` repository (`./intellij-rust`) provides a reference implementation of a production-grade, highly scalable language plugin. Key reference files include:

1. **Parser & Grammar Rules**:
   - `intellij-rust/src/main/grammars/RustParser.bnf`: Grammar-Kit BNF specification demonstrating expression precedence, statement recovery, and declaration rules.
   - `intellij-rust/src/main/kotlin/org/rust/lang/core/parser/RustParserUtil.kt`: Custom `GeneratedParserUtilBase` extensions for Pratt expression parsing, custom parse hooks, and error recovery.
   - `intellij-rust/src/main/kotlin/org/rust/lang/core/parser/RsParserDefinition.kt`: Standard `ParserDefinition` implementing token set queries, file node creation, and element instantiation.
2. **AST & Element Types**:
   - `intellij-rust/src/main/kotlin/org/rust/lang/core/psi/RsElementTypes.kt`: Factory defining token sets (`KEYWORDS`, `OPERATORS`) and `IElementType` constants.
   - `intellij-rust/src/main/kotlin/org/rust/lang/core/psi/RsCompositeElementType.kt`: Base element type wrapping AST nodes into PSI elements.
3. **PSI Hierarchy & Declarations**:
   - `intellij-rust/src/main/kotlin/org/rust/lang/core/psi/ext/RsElement.kt`: Base PSI interface for language elements.
   - `intellij-rust/src/main/kotlin/org/rust/lang/core/psi/ext/RsNamedElement.kt`: Standard implementation of `PsiNamedElement` and `PsiNameIdentifierOwner` for named declarations (`getName()`, `setName()`, `getNameIdentifier()`).
   - `intellij-rust/src/main/kotlin/org/rust/lang/core/psi/ext/RsItemsOwner.kt`: Trait for container elements holding item declarations.
   - `intellij-rust/src/main/kotlin/org/rust/lang/core/psi/ext/RsExpr.kt`: Base interface for expression PSI elements.
4. **References & Resolve**:
   - `intellij-rust/src/main/kotlin/org/rust/lang/core/resolve/ref/RsReference.kt`: Interface extending `PsiPolyVariantReference`.
   - `intellij-rust/src/main/kotlin/org/rust/lang/core/resolve/ref/RsReferenceBase.kt`: Robust base implementation for single/multi-variant reference resolution.
   - `intellij-rust/src/main/kotlin/org/rust/lang/core/resolve/ref/RsPathReferenceImpl.kt`: Implements reference resolution for qualified path references.
5. **IDE Features (Completion, Find Usages, Formatter, Tests)**:
   - `intellij-rust/src/main/kotlin/org/rust/lang/core/completion/RsCompletionContributor.kt`: Keyword and declaration completion contributor.
   - `intellij-rust/src/main/kotlin/org/rust/ide/search/RsFindUsagesProvider.kt`: Declarative `FindUsagesProvider` implementation.
   - `intellij-rust/src/main/kotlin/org/rust/ide/formatter/RsFormattingModelBuilder.kt`: Block formatting model builder.
   - `intellij-rust/src/test/kotlin/org/rust/lang/core/parser/RsParsingTestCase.kt`: Test base extending IntelliJ `ParsingTestCase`.

### IntelliJ Elixir

The `intellij-elixir` repository (`./intellij-elixir`) provides a clean reference for language plugins without excessive compiler infrastructure. Key reference files include:

1. **Parser & Grammar Rules**:
   - `intellij-elixir/src/org/elixir_lang/Elixir.bnf`: Clean Grammar-Kit BNF specification for expression and declaration rules.
   - `intellij-elixir/src/org/elixir_lang/ElixirParserDefinition.kt`: Concise Kotlin implementation of `ParserDefinition`.
   - `intellij-elixir/src/org/elixir_lang/parser/`: Hand-crafted parser rules for calls and macro expressions.
2. **AST & Element Types**:
   - `intellij-elixir/src/org/elixir_lang/psi/ElixirElementType.kt`: Lightweight `IElementType` base class.
3. **PSI Hierarchy & Named Elements**:
   - `intellij-elixir/src/org/elixir_lang/psi/impl/PsiElementImpl.kt`: Simple `ASTWrapperPsiElement` base extension.
   - `intellij-elixir/src/org/elixir_lang/psi/impl/PsiNamedElementImpl.kt`: Lightweight `PsiNamedElement` wrapper without stub dependencies.
   - `intellij-elixir/src/org/elixir_lang/psi/NamedElement.kt`: Interface joining `PsiNamedElement` and `NavigationItem`.
4. **References & Scoped Resolve**:
   - `intellij-elixir/src/org/elixir_lang/reference/Callable.kt`: Resolves function and variable calls via local scope walking.
   - `intellij-elixir/src/org/elixir_lang/psi/scope/Module.kt`: Scoped module symbol resolution.
5. **IDE Features (Completion, Find Usages, Formatter, Tests)**:
   - `intellij-elixir/src/org/elixir_lang/code_insight/completion/contributor/Keywords.kt`: Pattern-based keyword completion.
   - `intellij-elixir/src/org/elixir_lang/find_usages/Provider.kt`: Lightweight `FindUsagesProvider`.
   - `intellij-elixir/src/org/elixir_lang/formatter/ModelBuilder.kt`: Block-based code formatting model builder.
   - `intellij-elixir/tests/org/elixir_lang/parser/ParsingTestCase.java`: Standard JUnit/ParsingTestCase base.

### Shared IntelliJ Patterns

1. **`ParserDefinition` Binding**: Single entry point binding `Lexer`, `PsiParser`, `IFileElementType`, and comment/string tokensets.
2. **`PsiNamedElement` + `PsiNameIdentifierOwner`**: Universal contract for all named declarations (`circuit`, `witness`, `contract`, `struct`, `enum`, `type`, `ledger`, `module`).
3. **AST-to-PSI Wrapping**: Delegating `createElement(ASTNode)` in `ParserDefinition` to custom `ASTWrapperPsiElement` implementations based on `ASTNode.getElementType()`.
4. **`PsiPolyVariantReference` / `PsiReferenceBase`**: Standard framework for symbol resolution handling single and multiple declaration matches.
5. **`ParsingTestCase` Infrastructure**: Automated test runner reading `.compact` input files and asserting against `.txt` expected parse trees.

### Recommended Compact Patterns

1. **Handwritten PSI & Element Types**: Define custom `IElementType` constants in `CompactElementTypes` and handwritten PSI wrapper classes/interfaces (e.g. `CompactCircuitDefinition`, `CompactStructDefinition`) extending `ASTWrapperPsiElement`.
2. **Unified Named Declaration Base (`CompactNamedElement`)**: Create an interface `CompactNamedElement` extending `PsiNamedElement` and `PsiNameIdentifierOwner` for all Compact declaration PSI nodes.
3. **Scope-Walking References (`CompactReferenceBase`)**: Follow Elixir's lightweight scope-walking pattern for local variables, parameters, and contract fields rather than heavy stub indexes.
4. **Pattern-Based Completion (`CompactCompletionContributor`)**: Implement keyword and type completion using IntelliJ `CompletionContributor` and `PsiElementPattern`.

### Patterns to Avoid (Overengineering for Compact)

1. **Complex Macro Expansion Engine**: Rust's macro expansion system (`org.rust.lang.core.macros`) is unnecessary for Compact.
2. **Cross-Crate Stub Indexing (`StubBasedPsiElement`)**: Rust stubs every item across crates for global index resolution. Compact contracts are typically single-file or multi-file project files where AST scope walking is fast and sufficient.
3. **Compiler DFA / Type Inference Engine**: Rust's dataflow and borrow checker analysis (`org.rust.lang.core.dfa`) is compiler logic; IDE plugin should focus strictly on syntax, PSI, references, and completion.

### Compact → IntelliJ Mapping

| Upstream Compact Concept (`parser.ss` / `Lsrc`) | Upstream AST Node | Proposed IntelliJ Element Type | PSI Class / Interface | IntelliJ Pattern |
| :--- | :--- | :--- | :--- | :--- |
| Program Top-level | `Program` | `FILE` (`IFileElementType`) | `CompactFile` | `PsiFileNode` / `FileViewProvider` |
| Pragma Form | `pdecl` | `PRAGMA_FORM` | `CompactPragmaForm` | `ASTWrapperPsiElement` |
| Include Declaration | `incld` | `INCLUDE_DECLARATION` | `CompactIncludeDeclaration` | `PsiReference` (File Reference) |
| Module Definition | `mdefn` | `MODULE_DEFINITION` | `CompactModuleDefinition` | `CompactNamedElement`, `PsiNameIdentifierOwner` |
| Import Declaration | `idecl` | `IMPORT_DECLARATION` | `CompactImportDeclaration` | `PsiReference` (Module Reference) |
| Export Declaration | `xdecl` | `EXPORT_DECLARATION` | `CompactExportDeclaration` | Container PSI element |
| Ledger Declaration | `ldecl` | `LEDGER_DECLARATION` | `CompactLedgerDeclaration` | `CompactNamedElement`, `PsiNameIdentifierOwner` |
| Ledger Constructor | `lconstructor` | `CONSTRUCTOR_DECLARATION` | `CompactConstructorDeclaration` | Block container PSI element |
| Circuit Definition | `cdefn` | `CIRCUIT_DEFINITION` | `CompactCircuitDefinition` | `CompactNamedElement`, `PsiNameIdentifierOwner` |
| Witness Declaration | `wdecl` | `WITNESS_DECLARATION` | `CompactWitnessDeclaration` | `CompactNamedElement`, `PsiNameIdentifierOwner` |
| Contract Implements | `cidecl` | `CONTRACT_IMPLEMENTS_DECLARATION` | `CompactContractImplementsDeclaration` | `PsiReference` (Type Reference) |
| External Contract | `ecdecl` | `EXTERNAL_CONTRACT_DECLARATION` | `CompactExternalContractDeclaration` | `CompactNamedElement`, `PsiNameIdentifierOwner` |
| Struct Definition | `structdef` | `STRUCT_DEFINITION` | `CompactStructDefinition` | `CompactNamedElement`, `PsiNameIdentifierOwner` |
| Enum Definition | `enumdef` | `ENUM_DEFINITION` | `CompactEnumDefinition` | `CompactNamedElement`, `PsiNameIdentifierOwner` |
| Type Definition | `tdefn` | `TYPE_DEFINITION` | `CompactTypeDefinition` | `CompactNamedElement`, `PsiNameIdentifierOwner` |
| Block Statement | `blck` | `BLOCK_STATEMENT` | `CompactBlockStatement` | Scoped container PSI element |
| Statements | `stmt` | `IF_STATEMENT`, `FOR_STATEMENT`, `CONST_STATEMENT`, `RETURN_STATEMENT`, `EXPR_STATEMENT` | `CompactStatement` | Statement PSI hierarchy |
| Expressions | `expr` | `BINARY_EXPR`, `UNARY_EXPR`, `CALL_EXPR`, `INDEX_EXPR`, `MEMBER_EXPR`, `TUPLE_EXPR`, `NEW_EXPR`, `CAST_EXPR`, `TERNARY_EXPR` | `CompactExpression` | Expression PSI hierarchy & Pratt parsing |
| Types | `type` | `TYPE_REF`, `BUILTIN_TYPE`, `TUPLE_TYPE` | `CompactType` | Type PSI hierarchy |

### Important Reference Files

| Area | Reference File Path | Why Relevant to Compact |
| :--- | :--- | :--- |
| **Parser** | `intellij-rust/src/main/kotlin/org/rust/lang/core/parser/RustParserUtil.kt` | Pratt parsing and custom PsiBuilder recovery helpers. |
| **ParserDefinition** | `intellij-elixir/src/org/elixir_lang/ElixirParserDefinition.kt` | Clean, concise Kotlin implementation of `ParserDefinition`. |
| **Element Types** | `intellij-rust/src/main/kotlin/org/rust/lang/core/psi/RsElementTypes.kt` | Structure for organizing token sets and element type factories. |
| **PSI Base** | `intellij-rust/src/main/kotlin/org/rust/lang/core/psi/ext/RsElement.kt` | Clean base interface for all custom language PSI elements. |
| **Named Declarations** | `intellij-rust/src/main/kotlin/org/rust/lang/core/psi/ext/RsNamedElement.kt` | Standard `PsiNamedElement` + `PsiNameIdentifierOwner` pattern for declarations (`circuit`, `struct`, `contract`, etc.). |
| **Simple Named Elements** | `intellij-elixir/src/org/elixir_lang/psi/impl/PsiNamedElementImpl.kt` | Simple wrapper implementation of `PsiNamedElement` without stub dependencies. |
| **References** | `intellij-rust/src/main/kotlin/org/rust/lang/core/resolve/ref/RsReferenceBase.kt` | Robust base class for single and poly-variant reference resolution. |
| **Scoped Resolve** | `intellij-elixir/src/org/elixir_lang/reference/Callable.kt` | Clean scope-walking reference resolver for local variables and functions. |
| **Completion** | `intellij-rust/src/main/kotlin/org/rust/lang/core/completion/RsCompletionContributor.kt` | Pattern-based keyword and declaration completion contributor. |
| **Find Usages** | `intellij-rust/src/main/kotlin/org/rust/ide/search/RsFindUsagesProvider.kt` | Declarative `FindUsagesProvider` for named elements and keywords. |
| **Inspections** | `intellij-rust/src/main/kotlin/org/rust/ide/inspections/RsLocalInspectionTool.kt` | Base inspection class for AST-walking code inspections. |
| **Formatter** | `intellij-rust/src/main/kotlin/org/rust/ide/formatter/RsFormattingModelBuilder.kt` | Block-based code formatting model builder. |
| **Tests** | `intellij-rust/src/test/kotlin/org/rust/lang/core/parser/RsParsingTestCase.kt` | Standard `ParsingTestCase` setup for automated `.compact` -> parse tree testing. |

## Phase 4 Architecture — Semantic Intelligence

Phase 4 implements the semantic layer for the Midnight Compact plugin, providing type inference, semantic highlighting, and code inspections.

### Type System & Inference
- **Core Types:** Modeled via `CompactType` and `CompactPrimitiveType`.
- **Expression Interface:** `CompactExpression` provides `getType()` for all expression nodes.
- **Inference Strategy:** Innermost-first local inference using `CompactResolveUtil` for symbol lookup and `CompactStructFieldReference` for member access.
- **Safety:** Always returns `CompactPrimitiveType.UNKNOWN` for unresolved or malformed code.

### Semantic Model (Planned)
- Maps PSI declarations to durable `CompactSymbol` objects.
- Decouples resolution logic from PSI traversal where possible.

### Highlighting & Inspections (Planned)
- Uses `Annotator` for semantic token coloring.
- Inspections target unresolved references, type mismatches, and duplicate declarations via `CompactVisitor`.

### Implementation Files
- `CompactType.java`, `CompactPrimitiveType.java`: Core type definitions.
- `CompactExpression.java`, `CompactTypeElement.java`: PSI interfaces for type-awareness.
- `CompactStructFieldReference.java`: Type-aware field resolution.

## Next Task
Implement semantic highlighting using `CompactSemanticHighlightingAnnotator`.

## Technical Debt

- `README.md` still contains mostly generated IntelliJ plugin template content.
- Old parser fixture files under `src/test/testData/pragma` still describe broader `reference.bnf` version-expression
  behavior than the active handwritten parser supports.
- Compiler file review history needs exact upstream file names and feature notes as future compiler files are analyzed.

## Lessons Learned

- Keep `reference.bnf` and `Compact.flex` as grammar/lexer references for now; the active parser and lexer are
  handwritten and should be changed directly.
- Keep this notebook concise and focused on engineering continuity, not end-user README content.

## Session Log

### 2026-07-29

- Improved `src/main/grammar/Reference.bnf` against `references/compact-grammar.mdx`, cross-checked with
  `references/lsrc.json`: factored repeated Grammar-Kit rules, added targeted pins/recovery, preserved accepted syntax,
  and verified with `.\gradlew.bat build` from repo root.
- Synchronized the plugin with the `Lsrc.json` `External-Declaration` (`edecl`) and `Program-Element` sections by adding
  handwritten support for source `external` declarations.
- Changed `Reference.bnf` and `Compact.flex` so `external` is a real Compact keyword and parses as
  `export? external function-name type-param* arg* type` using the existing generic parameter, simple parameter list,
  and return type syntax; Kotlin token/highlighter wrappers were left unchanged until generated `CompactTypes` is
  refreshed.
- Initially verified `Lsrc.json` `External-Declaration` and `Program-Element` coverage against the plugin; the later
  generated grammar review added the missing `contract implements type;` keyword accuracy described below.
- Reconciled the broader source syntax references: `compact-grammar.mdx` documents `contract implements type;`, so
  `implements` is now a dedicated grammar/lexer token instead of being accepted through the broad `RESERVED_KEYWORD`
  token. This preserves the `implements_declaration` PSI rule name while making PSI regeneration add/use an `IMPLEMENTS`
  token.
- Noted a reference mismatch: `lsrc.json` includes standalone `External-Declaration` (`edecl`) in `Program-Element`,
  while `compact-grammar.mdx` does not list a standalone external declaration. The plugin keeps `external_declaration`
  because `lsrc.json` is the AST/source-language reference for that node, but this section is only partially
  synchronized across all references until the upstream grammar references agree.
- Created `AGENTS.md` as the permanent engineering notebook for the Midnight Compact IntelliJ plugin.
- Recorded current implemented plugin layers: lexer, syntax highlighting, Grammar-Kit BNF, parser, generated PSI, and
  parser definition.
- Recorded pending IDE features: references, completion, rename, find usages, type inference, inspections, and
  formatter.
- Debugged a Grammar-Kit parser failure where `export enum ...` after an import caused the parser to report
  `'import' unexpected`: generated `export_form` was pinned on `EXPORT`, so it committed before `enum_declaration`
  could parse exported declarations. Changed `export_form` in `Reference.bnf` to pin after `EXPORT LBRACE` (`pin=2`).
  Parser/PSI regeneration is still required because no Grammar-Kit generator task or jar is checked into the project.

### 2026-07-30

- Audited `Reference.bnf` against `references/compact-grammar.mdx`, `references/lsrc.json`, `references/lexer.ss`, and
  syntax examples in `references/type-example.compact`.
- Updated `nat` handling in `Reference.bnf` so plugin lexer tokens for decimal, hex, binary, and octal field literals
  are accepted everywhere the official compiler grammar consumes `nat`/`field`: version atoms, generic size arguments,
  type sizes, term field literals, `slice<...>`, and `pad(...)`.
- Fixed Grammar-Kit ordered-choice behavior for `if/else` statements by trying `stmt0` before the one-armed `if`
  fallback, preserving the official dangling-else grammar while avoiding a stranded `else` token.
- Verified with `.\gradlew.bat build`; parser/PSI regeneration is still required after the BNF changes.

### 2026-08-03

- Reconciled project documentation with the current handwritten architecture: `src/main/gen` is handwritten source in
  this project, while grammar/flex files are reference-only.
- Fixed the primary incremental PSI consistency risk by making `CompactParserDefinition.FILE` a stable static
  `IFileElementType` instead of returning a new file element type on every `getFileNodeType()` call.
- Hardened the handwritten `LexerBase` implementation: monotonic token offsets, explicit EOF token range, single-token
  malformed versions for `12.`, `12..`, and `12.a`, and no `FlexLexer`/adapter assumptions.
- Tightened the handwritten parser into a terminating top-level loop: parse pragma forms when possible, otherwise emit an
  error and advance exactly one token; recovery now stops at `;`, `pragma`, or EOF.
- Kept pragma parsing syntactic (`PRAGMA IDENTIFIER VERSION SEMICOLON`). Pragma-name validation belongs in a future
  inspection, not in parser tree construction.
- Replaced stale full-tree parser fixture assertions with focused parser/lexer regression tests for text preservation,
  malformed version tokens, incomplete input recovery, and EOF offsets.
- Verified with `.\gradlew.bat build`.

### 2026-08-11

- Audited `references/lexer.ss` against `CompactLexer.java`, `CompactTokenTypes.java`, and `CompactSyntaxHighlighter.java`.
- Fixed critical lexing bugs in `CompactLexer.java`:
  - `!=` operator lexing: added check for `=` after `!` to return `CompactTokenTypes.NEQ` instead of splitting into `NOT` and `ASSIGN`.
  - Range operator protection in number lexing (`1..10`): prevented double dot (`..`) after a decimal number from being consumed into an `INVALID_VERSION` token.
  - Generic nat hash parameter (`#`): added `case '#'` in `advance()` to emit `CompactTokenTypes.HASH`.
  - `implements` and `external` keywords: added to `KEYWORDS` map and `CompactTokenTypes.java` and highlighted as keywords.
  - Single-quoted string support: updated `lexString()` to accept both `'` and `"` delimiters.
  - Octal literal prefix: added `0o`/`0O` prefix parsing for `OCTAL_LITERAL`.
  - Identifier characters: allowed `$` in `isIdentifierStart` and `isIdentifierPart`.
  - Slash operator & comment handling: updated standalone `/` to emit `CompactTokenTypes.SLASH`.
- Added comprehensive unit test coverage in `LexerTest.java` and updated test fixtures.
- Verified test suite execution with `gradlew.bat test` (all 14 tests passing).
- Conducted deep investigation of official Compact compiler repository at `./compact`. Documented authoritative compiler locations, lexer (`lexer.ss`), parser (`parser.ss`), monadic combinator engine (`ez-grammar.ss`), CST/AST schemas (`Lparser`, `Lsrc`), error recovery mechanisms, test suites (`test.ss`, `test-contracts`), and syntax examples (`compact/examples/*.compact`).
- Added `## Compact Language Reference` and `## IntelliJ Parser Porting Notes` sections to `AGENTS.md`. Defined exact mappings for official AST nodes to IntelliJ PSI elements, expression precedence climbing strategy, PsiBuilder marker rollback strategies, and missing reserved keyword tokens.
- Implemented the handwritten Compact parser plan in `CompactParser.java`: top-level declaration dispatch, include/import/export/module/struct/enum/contract/implements/type/ledger/witness/constructor/circuit parsing, structured types/generics/patterns/parameters, block/statement parsing, and precedence-climbing expressions with generic-call versus relational `<` rollback.
- Added handwritten composite element types, shared `CompactTokenSets`, parser utilities, AST-to-PSI factory wiring, declaration PSI interfaces/implementations, `CompactNamedElement` support, `CompactBlock`, and `CompactReferenceExpr` for future references.
- Added parser and PSI regression coverage for declarations, names/name identifiers, types/generics/patterns, statements/blocks, expressions/ambiguity, recovery, factory consistency, and end-to-end parsing of `references/type-example.compact`.
- Fixed real-reference parser gaps found by `references/type-example.compact`: allow `Map<...>` in type context and avoid mis-parsing immediately-invoked lambdas like `(() => ...)()` as outer lambda parameter lists.
- Verified with `.\gradlew.bat build` (all tests passing).

### 2026-08-12

- Implemented Phase 3 editor navigation/code-intelligence foundation from `.junie/plans/phase-3-reference-resolution-completion.md` without changing the handwritten parser, lexer, token types, grammar references, or tests.
- Added single-file scope-walking resolution (`CompactResolveUtil`) with separate value/type namespaces, local shadowing, module export lookup, selected imports, and prefix flattening for `$name` imports.
- Added dedicated PSI wrappers for reference sites and named local/import elements; `CompactNamedElementImpl.setName()` now performs parsed identifier replacement for rename.
- Added `PsiReference` implementations for values, types, enum members, imports, struct literal names, and implements targets, enabling default Go To Declaration through platform reference resolution.
- Added Find Usages, NamesValidator, refactoring support, and contextual completion extension registrations in `plugin.xml`.
- Verified with `.\gradlew.bat build` (all tests passing).

### 2026-08-13

- Strengthened the failing rename/type-inference tests with explicit precondition assertions for struct type references
  and struct field declared types.
- Hardened `CompactNamedElementImpl.getNameIdentifier()` to select only direct identifier children, keeping rename
  anchored to declaration names rather than nested identifiers in composite declarations.
- Fixed struct field type inference to use the shared `CompactTypeElement` child lookup, so fields declared with builtin
  type nodes such as `Boolean` and `Field` return their actual declared type instead of `Unknown`.
- Implemented Phase 4 Type Inference system.
- Introduced `CompactType` and `CompactPrimitiveType` for language-level type representation.
- Refactored expression PSI to implement `CompactExpression` and `getType()`.
- Integrated `CompactTypeElement` into the named declaration hierarchy.
- Implemented inference for literals, references, and binary/unary operators.
- Added `CompactStructFieldReference` for type-based field resolution in `CompactMemberExpr`.
- Added `CompactTypeInferenceTest` verifying core inference logic and field resolution.
- Verified all changes with a suite of automated tests.

# Development Constraints

## Source of Truth

- Preserve the handwritten implementation.
- Use grammar and flex files as references only unless the project explicitly decides to regenerate code.

## Forbidden Files

Never edit any file under:

- build/**
- out/**
- .gradle/**
- .idea/**

## Grammar

If parser behavior needs to change:

1. Compare with `references/compact-grammar.mdx`; use `references/lsrc.json` when AST shape matters.
2. Modify the handwritten parser.
3. Keep `Reference.bnf` as reference documentation unless the change is specifically to reference material.

Grammar-Kit notes:

- Preserve official Compact syntax exactly; do not simplify ambiguous syntax without proof from references.
- Prefer factoring repeated prefixes, removing left recursion, and adding targeted `pin` / `recoverWhile`.
- Be careful pinning generic arguments after `<`; comparisons like `a < b` need backtracking.

## Lexer

If tokenization changes:

1. Compare with `references/lexer.ss` and `src/main/grammar/Compact.flex`.
2. Modify the handwritten `LexerBase` implementation.
3. Keep `.flex` files as reference documentation unless the change is specifically to reference material.

## PSI

If PSI needs new elements:

- Add or update handwritten AST element types and PSI wrappers.
- Keep wrapper creation compatible with IntelliJ incremental parsing.

## Existing Code

Prefer modifying existing handwritten files rather than creating new ones.

Avoid duplicate implementations.

Reuse existing utilities whenever possible.

## Before Creating Files

Before creating any new file:

1. Search the project for an existing implementation.
2. Extend it if appropriate.
3. Create a new file only if no suitable location exists.

## Before Changing Code

Always verify:

- the feature is not yet implemented
- the compiler file actually requires the change
- the change belongs in an IntelliJ plugin rather than the compiler

## Allowed Locations

Handwritten source only:

- src/main/kotlin/**
- src/main/java/**
- src/main/resources/**
- src/main/grammars/**

## Scope

Only implement features that affect the editor:

- parsing
- PSI
- references
- completion
- rename
- find usages
- inspections
- formatting

Do not implement compiler passes, optimizations, lowering, code generation, or runtime behavior.

## Every Change Must

- compile successfully
- preserve existing functionality
- minimize modified files
- include an explanation of why the change was necessary

# References

- the references directory includes files from the official code base.
