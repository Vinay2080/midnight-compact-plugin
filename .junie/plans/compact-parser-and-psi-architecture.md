---
sessionId: session-260811-201859-10o4
---

# Overview

### Architecture Overview

This is a **planning-only** deliverable for building the complete handwritten Compact parser (Stage 1) and the PSI architecture that sits on top of it (Stage 2), plus the explicit Parser ↔ PSI contract and forward-compatibility for future IDE features (Stage 3). No code is implemented here.

#### Confirmed foundational decisions
- **Language:** all new parser + PSI code stays in **Java**, under the existing package `dev.verloren.midnight`, matching the current lexer/parser/PSI style. No Kotlin, no build changes.
- **Expression parsing:** a single **precedence-climbing (Pratt)** core with prefix/postfix handlers, instead of one method per grammar level. This matches the AGENTS.md porting notes and avoids deep recursion.
- **PSI granularity:** **interface + impl per named declaration** (circuit, witness, ledger, struct, enum, type, module, external contract) sharing a common `CompactNamedElement`; expressions/types/statements start on the generic wrapper and are promoted only when a feature needs them.

#### Current state (verified in source)
- `CompactLexer.java` — **complete**: full tokenization (keywords, builtin types, literals, operators, comments, versions, malformed-version/unterminated recovery).
- `CompactParser.java` — **only parses `pragma`**; top level errors-and-advances on everything else.
- `CompactElementTypes.java` — **only `PRAGMA_FORM`**.
- `CompactParserDefinition.java` — wired, stable `IFileElementType FILE`, comment token set; `createElement` returns the generic `CompactPsiElement` for every node.
- `CompactPsiElement.java` / `CompactFile.java` — generic `ASTWrapperPsiElement` and `PsiFileBase`.
- Tests: `PragmaParserTest` uses `ParsingTestCase` with `src/test/testData`; `LexerTest`, `PragmaTest` exist.

**Conclusion:** the lexer is a solid foundation; Stages 1 and 2 are essentially greenfield above the lexer and must be designed together.

#### Authoritative reference (verified)
- `references/compact-grammar.mdx` — the **full official EBNF** (Compact 0.25.103): program-element list, pragma/version, include, import/export, module, struct, enum, external contract, `contract implements`, type alias, ledger, witness, constructor, circuit, types, generics, patterns, statements, and the **10-level expression precedence hierarchy** (`expr` → `expr0..expr9` → `term`) with ternary/assignment on top.
- `references/parser.ss`, `references/lexer.ss`, `references/lsrc.json` — AST shape / tokenization cross-checks.

#### AGENTS.md discrepancies to flag (not silently changed)
1. **Empty reference submodules.** `./compact`, `./intellij-rust`, `./intellij-elixir` are present as **empty directories** locally (uncheckout submodules). Every AGENTS.md path like `compact/compiler/parser.ss`, `compact/examples/*.compact`, `intellij-rust/.../RustParserUtil.kt` **does not exist on disk**. The real authoritative material is in `references/` (`parser.ss`, `lexer.ss`, `lsrc.json`, `compact-grammar.mdx`). The plan relies on `references/` and the platform patterns described in AGENTS.md rather than the missing repos.
2. **No `src/main/gen` and no `src/main/kotlin`.** AGENTS.md 'Current Architecture'/'Allowed Locations' mentions `src/main/gen` ("handwritten source") and `src/main/kotlin/**`; neither exists. All handwritten source is under `src/main/java/dev/verloren/midnight/**`. The plan uses that location.
3. **Official Compact/error example fixtures are unavailable locally.** Testing therefore uses `references/type-example.compact` plus purpose-built fixtures; a checkpoint notes swapping in official fixtures if the `compact/` submodule is later checked out.

# Stage 1 — Parser

### Stage 1 — Compact Parser Plan

Handwritten recursive-descent parser on IntelliJ `PsiBuilder`, preserving the official grammar from `references/compact-grammar.mdx`. Marker-based backtracking replaces the monadic `++` ordered choice of `parser.ss`. **Not a blind translation** of `parser.ss` — it is adapted to PsiBuilder idioms (`mark()`, `done()`, `rollbackTo()`, `error()`, `advanceLexer()`).

#### Entry point & top level
- `CompactParser.parse` already opens the file marker and loops to EOF. Replace the pragma-only body with a `parseProgramElement` dispatcher keyed on the leading token, consuming a leading `export` where the grammar allows it and peeking the next keyword to choose the rule.
- **Top-level recovery:** on an unrecognized/failed element, `error(...)` then skip tokens until a *synchronizing* token — one of `;`, or a declaration keyword (`pragma import export include module contract circuit struct enum type ledger witness constructor sealed pure`) or EOF. This bounds damage to a single declaration (mirrors AGENTS.md §5).

#### Grammar-construct table
For every construct: official rule (grammar mdx) · tokens consumed · parser method · resulting `IElementType` · expected PSI node · recovery · tests.

 Construct | Grammar rule | Key tokens | Parser method | IElementType | PSI node | Recovery |
---|---|---|---|---|---|---|
 Program | *program* | — | `parse` (loop) | `FILE` | `CompactFile` | sync to decl kw / `;` |
 Pragma | *pragma-form* | `PRAGMA` id ver `;` | `parsePragma` (exists) | `PRAGMA_FORM` | `CompactPragmaForm` | to `;`/`pragma` |
 Include | *include-form* | `INCLUDE` `STRING_LITERAL` `;` | `parseInclude` | `INCLUDE_FORM` | `CompactIncludeDeclaration` | to `;` |
 Import | *import-form* | `IMPORT` sel? name gargs? prefix? `;` | `parseImport` | `IMPORT_FORM`,`IMPORT_SELECTION`,`IMPORT_ELEMENT`,`IMPORT_PREFIX` | `CompactImportDeclaration` | to `;` |
 Export | *export-form* | `EXPORT` `{` id,… `}` `;`? | `parseExportForm` | `EXPORT_FORM` | `CompactExportDeclaration` | to `;`/`}` |
 Module | *module-definition* | `EXPORT`? `MODULE` id gparams? `{` … `}` | `parseModule` | `MODULE_DEFINITION` | `CompactModuleDefinition` | to `}`/decl kw |
 Struct | *struct-declaration* | `EXPORT`? `STRUCT` id gparams? `{` typed-id;… `}` | `parseStruct` | `STRUCT_DECLARATION`,`STRUCT_FIELD` | `CompactStructDefinition` | field: to `;`/`,`/`}` |
 Enum | *enum-declaration* | `EXPORT`? `ENUM` id `{` id,…¹ `}` | `parseEnum` | `ENUM_DECLARATION`,`ENUM_MEMBER` | `CompactEnumDefinition` | to `,`/`}` |
 External contract | *contract-declaration* | `EXPORT`? `CONTRACT` id `{` circuit-decl;… `}` | `parseExternalContract` | `CONTRACT_DECLARATION`,`EXTERNAL_CIRCUIT` | `CompactExternalContractDeclaration` | to `;`/`}` |
 Implements | *implements-declaration* | `CONTRACT` `IMPLEMENTS` type `;` | `parseImplements` | `IMPLEMENTS_DECLARATION` | `CompactContractImplementsDeclaration` | to `;` |
 Type alias | *type-alias-declaration* | `EXPORT`? `NEW`? `TYPE` id gparams? `=` type `;` | `parseTypeAlias` | `TYPE_ALIAS_DECLARATION` | `CompactTypeDefinition` | to `;` |
 Ledger | *ledger-declaration* | `EXPORT`? `SEALED`? `LEDGER` id `:` type `;` | `parseLedger` | `LEDGER_DECLARATION` | `CompactLedgerDeclaration` | to `;` |
 Witness | *witness-declaration* | `EXPORT`? `WITNESS` id gparams? simple-params `:` type `;` | `parseWitness` | `WITNESS_DECLARATION` | `CompactWitnessDeclaration` | to `;` |
 Constructor | *constructor-definition* | `CONSTRUCTOR` pattern-params block | `parseConstructor` | `CONSTRUCTOR_DEFINITION` | `CompactConstructorDeclaration` | to `}` |
 Circuit | *circuit-definition* | `EXPORT`? `PURE`? `CIRCUIT` id gparams? pattern-params `:` type block | `parseCircuit` | `CIRCUIT_DEFINITION` | `CompactCircuitDefinition` | to `}` |

**Shared sub-rules** (parser methods → element types): `parseGenericParameterList`→`GENERIC_PARAMETER_LIST`/`GENERIC_PARAMETER` (`#`? tvar); `parseGenericArgumentList`→`GENERIC_ARGUMENT_LIST`/`GENERIC_ARGUMENT` (nat|type); `parseSimpleParameterList`→`SIMPLE_PARAMETER_LIST`; `parsePatternParameterList`→`PATTERN_PARAMETER_LIST`; `parseArrowParameterList`→`ARROW_PARAMETER_LIST`; `parseTypedId`→`TYPED_ID`; `parseTypedPattern`→`TYPED_PATTERN`; `parseOptionallyTypedPattern`→`OPTIONALLY_TYPED_PATTERN`; `parsePattern`→`PATTERN`/`PATTERN_STRUCT_ELEMENT`; `parseReturnType`→`RETURN_TYPE`.

#### Types (`parseType`)
- `TYPE_REFERENCE` = `id` `gargs?`; builtins → `BUILTIN_TYPE` including `Uint<tsize>`, `Uint<tsize..tsize>`, `Bytes<tsize>`, `Opaque<str>`, `Vector<tsize,type>`; `[type,…]` → `TUPLE_TYPE`; `TYPE_SIZE` = nat | id. Recovery: to `>`/`,`/`)`/`;`.

#### Statements & blocks
- `parseBlock`→`BLOCK` (`{` stmt* `}`), recovery to `}`.
- `parseStatement`: `IF_STATEMENT` (dangling-else handled by trying the else-form before the one-armed `if`, per AGENTS.md 2026-07-30 note), `FOR_STATEMENT` (`for(const id of a..b)` and `for(const id of expr)`), `CONST_STATEMENT` (`const` cbinding,…¹ `;`) with `CONST_BINDING`, `RETURN_STATEMENT` (`return expr-seq? ;`), `EXPR_STATEMENT` (`expr-seq ;`), nested `BLOCK`. `EXPRESSION_SEQUENCE` wraps comma-separated exprs. Statement recovery: to `;` or `}`.

#### Expressions — precedence climbing (the core)
Single `parseExpression(minPrecedence)` loop driven by a binary-operator precedence table, wrapping left-assoc `BINARY_EXPR` via `precedingMarker.done()`.
- **Ternary + assignment** (lowest, right-assoc): after parsing an `expr0`, if `?` → `TERNARY_EXPR`; if `=`/`+=`/`-=` → `ASSIGN_EXPR` (right-recursive).
- **Binary levels** (table): `||`(0) `&&`(1) `== !=`(2) `< <= >= >`(3) then **cast** `as`(4)→`CAST_EXPR` `+ -`(5) `* /`(6). Left-assoc; relational level non-chaining as in grammar.
- **Unary prefix** `!`(7)→`UNARY_EXPR` (right-assoc).
- **Postfix**(8): `[expr]`→`INDEX_EXPR`, `.id`→`MEMBER_EXPR`, `.id(args)`→method `CALL_EXPR`; loop while postfix tokens present.
- **expr9 primaries**: `fun(args)`→`CALL_EXPR`, `map(...)`→`MAP_EXPR`, `fold(...)`→`FOLD_EXPR`, `slice<tsize>(...)`→`SLICE_EXPR`, `[tuple-arg,…]`→`TUPLE_EXPR`, `Bytes[...]`→`BYTES_EXPR`, `tref{struct-arg,…}`→`STRUCT_LITERAL_EXPR`, `assert(...)`→`ASSERT_EXPR`, `emit(...)`→`EMIT_EXPR`, `disclose(...)`→`DISCLOSE_EXPR`.
- **term**: `id`→`REFERENCE_EXPR`, `true/false/nat/str`→`LITERAL_EXPR`, `pad(nat,str)`→`PAD_EXPR`, `default<type>`→`DEFAULT_EXPR`, `(expr-seq)`→`PAREN_EXPR`. Arrow functions (`fun` = arrow-params ret? `=>` block|expr) → `LAMBDA_EXPR`. `TUPLE_ARG`/`STRUCT_ARG` cover `...` spread and `id: expr`.

#### Critical ambiguity — `<` generics vs relational (AGENTS.md §7)
- **Type context** (after `:`, `as`, inside `Uint/Bytes/Vector/Opaque/default/slice<...>`, type alias RHS, gparams/gargs): `<` is always generics.
- **Expression context**: after an `id` term, opening `<` is ambiguous between a function-reference generic-arg list (`f<T>(...)` / `tref<T>{...}`) and relational `a < b`. **Resolution:** set a `PsiBuilder.Marker`, speculatively parse a `GENERIC_ARGUMENT_LIST`; **commit only if** it closes with `>` *and* is immediately followed by `(` (call) or `{` (struct literal). Otherwise `rollbackTo()` and treat `<` as the relational operator at precedence level 3. Helper lives in `CompactParserUtil`.

#### Backtracking / incomplete / malformed handling
- All ordered-choice points use `Marker` + `rollbackTo()`; each rule *pins* after enough tokens to avoid runaway rollback.
- Every loop guarantees forward progress (advance ≥1 token on error) to prevent hangs (regression already covered by `testHangTest`).
- Incomplete input: missing closers/terminators produce `error(...)` but keep the enclosing node so the tree remains editable for incremental reparse.

#### Parser helper module
`CompactParserUtil` (new): `at`/`expect`/`errorAndAdvance`/`sync(TokenSet)` recovery, precedence table + `binaryPrecedence(token)`, `parseCommaList(...)` with optional trailing separator, and `tryParseGenericArguments(builder)` lookahead. Keeps `CompactParser` readable.

# Stage 2 — PSI

### Stage 2 — PSI Architecture Plan

PSI wrappers over the AST produced by Stage 1, following IntelliJ Platform conventions and the Rust/Elixir patterns in AGENTS.md. **Cross-IDE platform APIs only** (`com.intellij.psi.*`, `com.intellij.extapi.psi.*`, `com.intellij.lang.*`) — no IDEA-specific APIs. No stub indexing (explicitly avoided per AGENTS.md 'Patterns to Avoid').

#### Base layer
- `CompactPsiElement` (exists) — generic `ASTWrapperPsiElement`; add `accept(CompactVisitor)` and keep it as the default node for structural/expression/type/statement nodes that don't yet need a dedicated class.
- `CompactVisitor extends PsiElementVisitor` (new) — `visitElement` fallbacks plus `visitXxx` hooks for declaration nodes; used later by inspections/annotators.
- `CompactNamedElement` (new interface) — `extends PsiNameIdentifierOwner, NavigationItem` (which pulls in `PsiNamedElement`). Contract for all named declarations: `getName()`, `setName()`, `getNameIdentifier()`, `getNameIdentifier()`-backed `getTextOffset()`.
- `CompactNamedElementImpl` (new abstract) — `extends CompactPsiElement implements CompactNamedElement`. Implements `getNameIdentifier()` by locating the declaration's `IDENTIFIER` child, `getName()` from its text, and `setName()` via element replacement/rename helper. All concrete declaration impls extend this.

#### Node classification

 Category | Nodes | PSI class / interface | Notes |
---|---|---|---|
 **Declarations (named)** | circuit, witness, ledger, struct, enum, type alias, module, external contract | dedicated interface + impl extending `CompactNamedElementImpl` | expose `getNameIdentifier()`; reference targets |
 **Declarations (structural)** | pragma, include, import, export, implements, constructor | dedicated interface + impl extending `CompactPsiElement` | not name owners; import/include hold future file/module refs |
 **Members** | struct field, enum member, external circuit, typed-id/pattern, params | mostly generic `CompactPsiElement`; struct field + enum member get thin named wrappers | field/member names matter for completion/refs |
 **Types** | type-reference, builtin, tuple, type-size | generic `CompactPsiElement` initially; `CompactTypeElement` marker interface | `TYPE_REFERENCE` promoted when Go-To-type lands |
 **Statements** | block, if, for, const, return, expr-stmt, const-binding | generic `CompactPsiElement`; `BLOCK` gets `CompactBlock` (scope holder) | block is the scope boundary for resolve |
 **Expressions** | all expr variants | generic `CompactPsiElement`; `CompactReferenceExpr` for `id` term | reference expr promoted when resolve lands |

**Do not** create a PSI class per token — leaf tokens stay as `LeafPsiElement` via the lexer; wrappers exist only for composite element types that carry meaning.

#### Concrete declaration interfaces + impls (Java, `dev.verloren.midnight.psi`)
`CompactPragmaForm`, `CompactIncludeDeclaration`, `CompactImportDeclaration`, `CompactExportDeclaration`, `CompactModuleDefinition`, `CompactStructDefinition`, `CompactEnumDefinition`, `CompactExternalContractDeclaration`, `CompactContractImplementsDeclaration`, `CompactTypeDefinition`, `CompactLedgerDeclaration`, `CompactWitnessDeclaration`, `CompactConstructorDeclaration`, `CompactCircuitDefinition`. Each is an interface (accessor contract) + `*Impl` class. Named ones extend `CompactNamedElement`/`CompactNamedElementImpl`; each exposes typed accessors (e.g. `CompactStructDefinition.getFields()`, `CompactCircuitDefinition.getParameterList()/getReturnType()/getBody()`, `CompactModuleDefinition.getGenericParameterList()/getMembers()`).

#### ParserDefinition integration
- New `CompactElementFactory.createElement(ASTNode)` — a `switch` on `node.getElementType()` returning the matching `*Impl` (or generic `CompactPsiElement` default). This is the single AST→PSI mapping point.
- Modify `CompactParserDefinition.createElement` to delegate to `CompactElementFactory`.
- Add `getWhitespaceTokens()` if needed and keep the stable `IFileElementType FILE` and comment token set.
- New `CompactTokenSets` (keywords/operators/builtin-types/literals) so the parser, highlighter, and future completion share one source of truth.

#### File PSI
`CompactFile` (exists) stays as `PsiFileBase`; add typed accessors used by future features: `getProgramElements()`, `getDeclarations()` (all `CompactNamedElement` children, recursively into modules).

# Parser↔PSI Contract

### Parser ↔ PSI Contract

Parser and PSI are designed together so implementing PSI never forces a parser rewrite.

#### Markers that create PSI nodes
Every `Marker.done(elementType)` in Stage 1 must use an element type from `CompactElementTypes`, and every non-generic element type must have a matching branch in `CompactElementFactory`. Leaf tokens are **not** wrapped. Nodes intentionally left generic (`CompactPsiElement`) are listed in the Stage 2 classification table.

#### IElementTypes required by the parser
All composite types enumerated in the Stage 1 table plus shared sub-rules and expression/type/statement variants. `CompactElementTypes` is the authoritative registry; the parser references only constants from it (never string names).

#### PSI classes wrapping nodes
Mapping is 1:1 through `CompactElementFactory`. Adding a dedicated PSI class later (promoting a generic node) requires **only** a new factory branch — no parser change — because the element type already exists.

#### Nodes that MUST preserve source ranges
All of them (mandatory for incremental reparse and editor features). Specifically: every declaration, every block/statement, every expression, and the file node. The parser must never drop tokens on error — it wraps or reports them so `file.getText()` round-trips exactly (already asserted in `PragmaParserTest`).

#### Nodes requiring stable names (`getNameIdentifier` + `IDENTIFIER` child)
circuit, witness, ledger, struct, enum, type alias, module, external contract (and struct field / enum member as thin named wrappers). The parser must place the declaration's name `IDENTIFIER` as a **direct child** of the declaration node so `CompactNamedElementImpl.getNameIdentifier()` can find it deterministically.

#### Nodes that must expose references later (design now, implement in Stage 3)
- `TYPE_REFERENCE` (`tref`) → type/struct/enum/module resolution.
- `REFERENCE_EXPR` (`id` term) and `MEMBER_EXPR` `.id` → variable/param/field/circuit resolution.
- `IMPORT_FORM`/`IMPORT_ELEMENT`/`INCLUDE_FORM` → module/file resolution.
- `IMPLEMENTS_DECLARATION` type → contract type resolution.
Parser guarantees each keeps its name `IDENTIFIER`/`STRING_LITERAL`/`TYPE_REFERENCE` as a locatable child, so a `PsiReferenceBase` can attach later without re-parsing.

#### Nodes that should remain generic
Punctuation-only groupings and non-semantic containers (parameter lists as raw containers, expression-sequence, tuple/struct arg wrappers) stay as `CompactPsiElement` until a feature needs them.

#### Scope-boundary contract
`BLOCK`, `MODULE_DEFINITION`, circuit/witness parameter lists, and `CompactFile` are the scope holders. Stage 3 resolve walks parent chain across these; the parser must nest them correctly (block inside circuit body, params as siblings of body).

# Stage 3 — Future IDE Features

### Stage 3 — Future IDE Feature Compatibility (design check only)

No implementation now. Each feature is mapped to the Stage 2 API that will support it, proving the architecture is sufficient.

 Feature | Supported by (Stage 2 API) | Notes |
---|---|---|
 **Go To Declaration** (next task) | `CompactNamedElement.getNameIdentifier()` on declarations + `PsiReferenceBase` attached to `REFERENCE_EXPR`/`TYPE_REFERENCE` | resolve target = nearest matching `CompactNamedElement` via parent-chain scope walk (Elixir pattern) |
 **Reference resolution** | `CompactReference extends PsiReferenceBase` over `REFERENCE_EXPR`, `TYPE_REFERENCE`, `IMPORT_ELEMENT`, `INCLUDE_FORM`, member `.id` | scope holders: `CompactBlock`, `MODULE_DEFINITION`, param lists, `CompactFile` |
 **Completion** | `CompactTokenSets` (keywords/types) + `CompactFile.getDeclarations()` + `PsiElementPattern` | keyword completion first, then declaration/field completion |
 **Rename** | `CompactNamedElement.setName()` + `getNameIdentifier()` | element-replacement rename; references updated via the reference layer |
 **Find Usages** | `FindUsagesProvider` + `CompactNamedElement` + `getNameIdentifier()` + word-scanner over `IDENTIFIER` | declarative provider (Rust/Elixir pattern) |
 **Inspections** | `CompactVisitor` + composite element types | AST-walking local inspections (e.g. unknown pragma name, duplicate binding) |
 **Type inference** | `CompactType*` interfaces + `RETURN_TYPE`/`TYPED_ID`/`CAST_EXPR` nodes | kept syntactic; deep inference is out-of-scope per AGENTS.md 'Patterns to Avoid' |
 **Formatter** | stable composite tree + `BLOCK`/delimiter tokens + `CompactTokenSets` | `FormattingModelBuilder` with block/indent rules on brace/paren/comma tokens |

**Gap check:** every feature depends only on (a) named-element APIs, (b) locatable name children, (c) correct scope nesting, and (d) shared token sets — all guaranteed by the Parser↔PSI contract. No feature requires re-parsing or new element types beyond promoting existing generic nodes.

# Testing

### Testing Strategy

Two separate suites, both using the existing `ParsingTestCase` infrastructure (`PragmaParserTest` is the template) and `src/test/testData`.

#### Parser tests (`src/test/java/dev/verloren/midnight/parser/`)
Approach: `.compact` input + expected parse-tree `.txt` (via `DebugUtil.psiToString`) fixtures, plus assertions on absence/presence of `PsiErrorElement` and exact `file.getText()` round-trip.
- **Valid syntax:** one fixture per declaration kind (pragma, include, import/export, module, struct, enum, external contract, implements, type alias, ledger, witness, constructor, circuit) — assert no `PsiErrorElement`.
- **Types & generics:** builtin types with sizes/ranges (`Uint<..>`, `Bytes<n>`, `Vector<n,T>`, `Opaque<str>`), tuple types, `tref<gargs>`, gparams with `#`.
- **Expressions & precedence:** golden trees proving `||`<`&&`<equality<relational<`as`<`+/-`<`*//`<`!`<postfix; ternary/assignment right-assoc; postfix chains; struct-literal, map/fold/slice, pad/default, arrow functions.
- **Generic vs relational ambiguity:** `a < b > c` (relational), `f<T>(x)` (call), `S<T>{...}` (struct literal), `a < b` — assert correct node types and correct rollback.
- **Invalid syntax:** missing `;`, missing name/type, bad tokens — assert `PsiErrorElement` present, correct error position, and single-declaration containment (recovery).
- **Incomplete syntax:** truncated declarations/blocks/expressions — assert no hang (bounded), tree still built, text round-trips.
- **Error recovery:** one bad declaration followed by a valid one — assert the following declaration still parses cleanly.
- **Official examples:** parse `references/type-example.compact` end-to-end (no error). *Checkpoint:* if the `compact/` submodule is later checked out, add `compact/examples/*.compact` (valid) and `compact/examples/errors/*.compact` (must produce errors).

#### PSI tests (`src/test/java/dev/verloren/midnight/psi/`)
Approach: `BasePlatformTestCase`/`ParsingTestCase` building PSI and asserting structure via platform APIs.
- **Tree structure & element types:** `PsiTreeUtil.findChildrenOfType` returns expected counts/types for each declaration.
- **PSI wrappers:** each declaration node instantiates its dedicated `*Impl` (via `CompactElementFactory`), not the generic wrapper.
- **Names & name identifiers:** `getName()` / `getNameIdentifier()` correct for every `CompactNamedElement`; `getNameIdentifier()` is the right `IDENTIFIER` child.
- **Parent/child relationships:** field→struct, member→enum, param→circuit, block→circuit; scope holders nest correctly.
- **Source ranges:** each node's `getTextRange()` matches its text; whole-file text round-trips.
- **Parser→PSI consistency:** every composite `IElementType` emitted by the parser resolves to a factory branch (guard test iterating declared element types).

#### Existing tests
`PragmaParserTest`, `LexerTest`, `PragmaTest` must keep passing (regression). Stale `testData/pragma` fixtures noted as tech debt in AGENTS.md are left as-is unless a test needs them.

# File-by-File

### File-by-File Implementation Plan

**Package root:** `dev.verloren.midnight`, language **Java**, location `src/main/java/dev/verloren/midnight/**`. No duplicate implementations; existing files are extended, not replaced.

#### New files
 Path | Lang | Type | Responsibility |
---|---|---|---|
 `parser/CompactParserUtil.java` | Java | final class | `at/expect/sync` recovery helpers, binary precedence table, comma-list parsing, `tryParseGenericArguments` lookahead |
 `psi/CompactElementFactory.java` | Java | final class | single AST→PSI `switch` on `IElementType`, returns `*Impl` or generic default |
 `psi/CompactVisitor.java` | Java | class | `PsiElementVisitor` with `visitXxx` hooks for declarations |
 `psi/CompactNamedElement.java` | Java | interface | `extends PsiNameIdentifierOwner, NavigationItem`; named-declaration contract |
 `psi/CompactNamedElementImpl.java` | Java | abstract class | base impl of `getName/setName/getNameIdentifier` over the name `IDENTIFIER` child |
 `psi/CompactTypeElement.java` | Java | interface | marker for type nodes (promotion point) |
 `psi/CompactBlock.java` | Java | class | scope-holder wrapper for `BLOCK` |
 `psi/CompactPragmaForm.java` (+Impl) | Java | interface+class | pragma accessors |
 `psi/CompactIncludeDeclaration.java` (+Impl) | Java | interface+class | include path accessor (future file ref) |
 `psi/CompactImportDeclaration.java` (+Impl) | Java | interface+class | import name/selection/prefix accessors |
 `psi/CompactExportDeclaration.java` (+Impl) | Java | interface+class | exported-id list |
 `psi/CompactModuleDefinition.java` (+Impl) | Java | interface+class | named; gparams + members |
 `psi/CompactStructDefinition.java` (+Impl) | Java | interface+class | named; fields |
 `psi/CompactEnumDefinition.java` (+Impl) | Java | interface+class | named; members |
 `psi/CompactExternalContractDeclaration.java` (+Impl) | Java | interface+class | named; external circuits |
 `psi/CompactContractImplementsDeclaration.java` (+Impl) | Java | interface+class | implemented type (future ref) |
 `psi/CompactTypeDefinition.java` (+Impl) | Java | interface+class | named; alias RHS type |
 `psi/CompactLedgerDeclaration.java` (+Impl) | Java | interface+class | named; field type |
 `psi/CompactWitnessDeclaration.java` (+Impl) | Java | interface+class | named; params + return type |
 `psi/CompactConstructorDeclaration.java` (+Impl) | Java | interface+class | params + body |
 `psi/CompactCircuitDefinition.java` (+Impl) | Java | interface+class | named; gparams, params, return type, body |
 `psi/CompactReferenceExpr.java` (+Impl) | Java | interface+class | `id` term (future resolve) |
 `lexer/CompactTokenSets.java` | Java | final class | shared `TokenSet`s (keywords/operators/builtin-types/literals) |

#### Modified files
 Path | Reason |
---|---|
 `parser/CompactParser.java` | replace pragma-only body with full recursive-descent + precedence-climbing parser and recovery |
 `parser/CompactElementTypes.java` | add all composite element types listed in Stage 1/2 (keep existing `PRAGMA_FORM`) |
 `parser/CompactParserDefinition.java` | delegate `createElement` to `CompactElementFactory` |
 `psi/CompactPsiElement.java` | add `accept(CompactVisitor)`; remains the generic default node |
 `psi/CompactFile.java` | add `getProgramElements()`/`getDeclarations()` accessors |
 `src/test/java/.../parser/*` , `src/test/java/.../psi/*` | new parser + PSI test classes and `testData` fixtures |

#### Explicitly NOT touched
`CompactLexer.java`/`CompactTokenTypes.java` (lexer complete), `src/main/grammar/*` (reference-only per AGENTS.md), `build/**`, `.idea/**`, `references/**`, empty `compact/`,`intellij-rust/`,`intellij-elixir/`. `plugin.xml` needs no change (parser/lexer already registered).

# Risks & Open Questions

### Risks & Unresolved Questions

#### Risks
- **`<` generic/relational ambiguity** — highest-risk item. Mitigation: speculative `Marker` parse committing only on `>` followed by `(`/`{`, else `rollbackTo()` to relational; covered by dedicated parser tests. Rollback must be bounded to avoid performance cliffs on pathological input.
- **Dangling `else`** — must try the else-form before the one-armed `if` (AGENTS.md already hit this with Grammar-Kit). Covered by tests.
- **Forward-progress / hangs** — every error path must advance ≥1 token; `testHangTest` pattern extended to new rules.
- **Incremental reparse correctness** — all nodes must preserve exact source ranges and round-trip `getText()`; stable `IFileElementType` already in place. Risk if error recovery drops tokens — tests assert round-trip.
- **PSI/parser drift** — mitigated by the `CompactElementFactory` guard test that every emitted element type has a factory branch.
- **Scope of expression grammar** — `fun`/arrow-function and struct-literal vs block `{` disambiguation is subtle; isolate in `expr9`/`term` with lookahead.

#### Open questions (do not block Stage 1/2, flagged for later)
1. **Official fixtures availability** — `compact/examples/*` are absent locally; should the `compact/` submodule be checked out so tests can consume the authoritative valid/error fixtures, or is `references/type-example.compact` + handwritten fixtures acceptable for now?
2. **`let` / reserved future keywords** — `CompactTokenTypes.LET` and the 36 reserved-for-future keywords (AGENTS.md §1) are not yet lexed as keywords. The grammar mdx does not use them; recommend keeping them as identifiers until an inspection needs them. Confirm no parser rule should treat `let` specially.
3. **Pragma-name validation** — currently enforced in the parser (`language_version`/`compiler_version`). AGENTS.md says this belongs in a future inspection; plan keeps existing behavior but flags it as inspection-candidate.
4. **Module-qualified references** (`prefix`, `import ... prefix X`) — resolution semantics deferred to Stage 3; Stage 1/2 only guarantee the name tokens are locatable children.

# Delivery Steps

### ✓ Step 1: Foundations: element types, PSI base, factory wiring
The AST→PSI infrastructure exists and compiles with the current pragma parser still working.

- Expand `CompactElementTypes.java` with every composite `IElementType` from the Stage 1/2 tables (declarations, shared sub-rules, types, statements, expressions), keeping existing `PRAGMA_FORM`.
- Add `lexer/CompactTokenSets.java` with shared keyword/operator/builtin-type/literal `TokenSet`s.
- Add PSI base layer: `CompactVisitor`, `CompactNamedElement` (interface), `CompactNamedElementImpl` (abstract), `CompactTypeElement` marker; extend `CompactPsiElement` with `accept(...)`.
- Add `psi/CompactElementFactory.java` (initially mapping only `PRAGMA_FORM` + generic default) and modify `CompactParserDefinition.createElement` to delegate to it.
- Add `parser/CompactParserUtil.java` skeleton (`at/expect/sync`, precedence table stub).
- Validation checkpoint: `.\gradlew.bat build` and existing `PragmaParserTest`/`LexerTest` pass unchanged.

### ✓ Step 2: Top-level declaration parsing + named-element PSI
All program-element declarations parse into their dedicated PSI nodes with working names.

- Rewrite `CompactParser.parse` dispatcher to route by leading token/`export`, with top-level `sync` recovery to declaration keywords / `;` / EOF.
- Implement `parseInclude`, `parseImport` (+selection/element/prefix), `parseExportForm`, `parseModule`, `parseStruct` (+fields), `parseEnum` (+members), `parseExternalContract` (+external circuits), `parseImplements`, `parseTypeAlias`, `parseLedger`, `parseWitness`, `parseConstructor`, `parseCircuit`.
- Add the corresponding declaration PSI interfaces + `*Impl` (named ones extend `CompactNamedElementImpl`) and register all in `CompactElementFactory`.
- Guarantee each declaration keeps its name `IDENTIFIER` as a direct child (contract requirement).
- Tests: valid-syntax fixture per declaration (no `PsiErrorElement`) and PSI name/nameIdentifier assertions.
- Validation checkpoint: build + new parser/PSI declaration tests pass.

### ✓ Step 3: Types, generics, patterns, parameter lists
Type references, builtin/tuple types, generic params/args, patterns and all parameter lists parse and nest correctly.

- Implement `parseType` (`TYPE_REFERENCE`, `BUILTIN_TYPE` incl. `Uint<..>`/`Bytes<n>`/`Vector<n,T>`/`Opaque<str>`, `TUPLE_TYPE`, `TYPE_SIZE`).
- Implement `parseGenericParameterList`/`parseGenericArgumentList` and the `#` tvar form.
- Implement `parsePattern` (+struct-element), `parseTypedId`, `parseTypedPattern`, `parseOptionallyTypedPattern`, `parseSimple/Pattern/ArrowParameterList`, `parseReturnType`.
- Wire these into circuit/witness/struct/type-alias/ledger from the previous stage; keep type nodes generic (`CompactTypeElement`) for now.
- Tests: generics with sizes/ranges, tuple types, gparams, and the pattern/parameter fixtures.
- Validation checkpoint: build + type/generic/pattern tests pass.

### ✓ Step 4: Statements and blocks
Circuit/constructor bodies parse full statement trees with correct scope nesting.

- Implement `parseBlock` (`CompactBlock` scope holder) with `}`-recovery.
- Implement `parseStatement`: if (with dangling-else handled by trying the else-form first), for (`of a..b` and `of expr`), const (+`CONST_BINDING`), return, expression-statement; plus `EXPRESSION_SEQUENCE`.
- Statement-level recovery to `;`/`}`.
- Register block/statement element types in `CompactElementFactory` (mostly generic wrappers except `CompactBlock`).
- Tests: valid statement fixtures, dangling-else, incomplete/truncated blocks (no hang, round-trip), recovery after a bad statement.
- Validation checkpoint: build + statement tests pass.

### ✓ Step 5: Expressions via precedence climbing
Full expression grammar parses with exact precedence/associativity and correct `<` disambiguation.

- Implement `parseExpression(minPrec)` precedence-climbing core using the `CompactParserUtil` table: `||`<`&&`<equality<relational<`as`(cast)<additive<multiplicative<unary`!`<postfix.
- Implement ternary + assignment (right-assoc) wrapper, postfix loop (`INDEX_EXPR`/`MEMBER_EXPR`/method `CALL_EXPR`), expr9 primaries (call, map/fold/slice, tuple/Bytes, struct-literal, assert/emit/disclose), term (`REFERENCE_EXPR`, literals, pad/default, paren), and arrow-function `LAMBDA_EXPR`.
- Implement `tryParseGenericArguments` lookahead: commit generics only on `>` followed by `(`/`{`, else rollback to relational `<`.
- Register expression element types in `CompactElementFactory`; promote `CompactReferenceExpr`.
- Tests: precedence golden trees, right-assoc ternary/assignment, postfix chains, and the full `a<b>c` / `f<T>(x)` / `S<T>{...}` ambiguity matrix.
- Validation checkpoint: build + expression/precedence/ambiguity tests pass.

### ✓ Step 6: End-to-end test suites and consistency guards
Comprehensive parser + PSI suites pass, including the reference example and a parser→PSI consistency guard.

- Add parser suite: valid, invalid, incomplete, recovery, and precedence fixtures under `src/test/testData`; parse `references/type-example.compact` with no errors.
- Add PSI suite: tree structure, wrapper-class identity via factory, names/nameIdentifiers, parent/child relationships, source-range round-trip.
- Add a guard test asserting every composite `IElementType` in `CompactElementTypes` has a `CompactElementFactory` branch (prevents parser/PSI drift).
- Note in tests where official `compact/examples/*` fixtures would slot in if the submodule is later checked out.
- Validation checkpoint: `.\gradlew.bat build` with all suites green; existing `PragmaParserTest`/`LexerTest`/`PragmaTest` still pass.