---
sessionId: session-260811-214745-15j2
---

# Requirements

### Overview & Goals

Design (not implement) **Phase 3** of the Midnight Compact IntelliJ plugin: the editor navigation & code-intelligence layer built on top of the already-complete Phase 2 Parser + PSI. Phase 3 delivers:

1. **References** — `PsiReference`s attached to Compact PSI nodes.
2. **Resolve** — a single-file scope-walking resolver.
3. **Go To Declaration** — via reference resolution (no dedicated handler needed).
4. **Find Usages** — `FindUsagesProvider` + word scanner.
5. **Rename** — via `CompactNamedElement.setName` + reference `handleElementRename`.
6. **Standard IntelliJ completion** — contextual `CompletionContributor` (not AI/full-line).

### Immediate Deliverables Of This Planning Task

- **Output 1:** `.junie/plans/phase-3-reference-resolution-completion.md` containing the full plan captured in these proposal tabs.
- **Output 2:** an `AGENTS.md` section `## Phase 3 Architecture — References, Resolution & Completion` with durable findings only (not the full plan).

No production source, parser, lexer, token, PSI or test files are modified during planning.

### Scope

**In Scope (Phase 3):**
- Single-file resolution: locals, parameters, block/const bindings, file- and module-level declarations, `import ... prefix $` name-flattening, `import { a as b } from M` selections within the same file.
- Value references (`REFERENCE_EXPR`), type references (`TYPE_REFERENCE`, struct-literal name, `default<T>`, `as T`, generic args, `implements` target), enum-member access (`Enum.member`), import-element references.
- Go To Declaration, Find Usages, Rename, and contextual Completion for the above.
- New dedicated `CompactNamedElement` PSI wrappers for parameters, struct fields, enum members, const bindings, generic parameters, and import aliases (per confirmed decision).

**Out of Scope (deferred / open questions):**
- Cross-file resolution of `include "file";` and file/module imports across `.compact` files (confirmed: **single-file first**).
- Resolution of `CompactStandardLibrary` builtins (Counter, Map, Set, Either, Maybe, ADT methods) and builtin primitive types — treated as **soft-unresolved** (no error) until a later phase.
- Struct **field access** resolution through `expr.field` and ledger ADT method resolution — requires type inference (Phase 4+).
- Destructuring/tuple pattern bindings as multi-name declarations (simple identifier patterns only).
- Type inference, inspections, formatter (later phases).

### User Stories

- As a Compact developer, I can Ctrl/Cmd-Click a circuit/witness/type/struct/enum/ledger/parameter/local name and jump to its declaration.
- As a developer, I can Find Usages of any named Compact declaration.
- As a developer, I can Rename a declaration and have its in-file references updated.
- As a developer, I get context-aware completion: types in type positions, in-scope values in expression positions, enum members after `Enum.`, and keywords at statement/declaration starts.

### Functional Requirements

- Unresolved references must degrade gracefully (return `null`/empty, no exceptions); builtin/stdlib names must **not** be flagged as errors by resolution.
- Resolution must respect **shadowing** (innermost binding wins) and **separate namespaces** for types vs values.
- Rename must reject Compact keywords and invalid identifiers (via `NamesValidator`).
- Completion must be **contextual**, never a blanket keyword dump where the PSI context makes the category clear.

### Non-Functional Requirements

- **Cross-IDE:** use only `com.intellij.psi.*`, `com.intellij.lang.*`, `com.intellij.codeInsight.*` and other platform APIs; avoid IDEA-specific APIs (flag any that leak in).
- **No indexing infrastructure** (no stubs / stub index) — single-file `PsiTreeUtil` scope walking (Elixir-style), matching the AGENTS.md "patterns to avoid" guidance.
- Preserve all existing handwritten parser/lexer/PSI behavior; additive changes only.

# Current State (Verified)

### Verified Phase 2 State (read from source, not assumed)

**Registered extensions** (`src/main/resources/META-INF/plugin.xml`): only `fileType`, `lang.parserDefinition`, `lang.syntaxHighlighterFactory`. No reference/find-usages/rename/completion EPs yet.

**Element types** (`parser/CompactElementTypes.java`) already exist for every node Phase 3 needs — **no new element types and no parser changes are required**:
- Declarations: `MODULE_DEFINITION`, `STRUCT_DECLARATION`, `STRUCT_FIELD`, `ENUM_DECLARATION`, `ENUM_MEMBER`, `CONTRACT_DECLARATION`, `IMPLEMENTS_DECLARATION`, `TYPE_ALIAS_DECLARATION`, `LEDGER_DECLARATION`, `WITNESS_DECLARATION`, `CONSTRUCTOR_DEFINITION`, `CIRCUIT_DEFINITION`.
- Bindings/params: `TYPED_ID`, `TYPED_PATTERN`, `OPTIONALLY_TYPED_PATTERN`, `PATTERN`, `GENERIC_PARAMETER`, `CONST_BINDING`.
- Imports: `IMPORT_FORM`, `IMPORT_SELECTION`, `IMPORT_ELEMENT`, `IMPORT_PREFIX`, `EXPORT_FORM`, `INCLUDE_FORM`.
- Types/exprs: `TYPE_REFERENCE`, `BUILTIN_TYPE`, `TUPLE_TYPE`, `REFERENCE_EXPR`, `MEMBER_EXPR`, `CALL_EXPR`, `STRUCT_LITERAL_EXPR`, `DEFAULT_EXPR`, `CAST_EXPR`, `INDEX_EXPR`, etc.

**PSI factory** (`psi/CompactElementFactory.java`): dedicated impls only for the big declarations + `BLOCK` + `REFERENCE_EXPR`. Everything else (`TYPE_REFERENCE`, `MEMBER_EXPR`, `STRUCT_LITERAL_EXPR`, `IMPORT_ELEMENT`, `TYPED_ID`, `STRUCT_FIELD`, `ENUM_MEMBER`, `CONST_BINDING`, `GENERIC_PARAMETER`) falls through to the generic `CompactPsiElement`.

**Named elements:** `CompactNamedElement extends PsiNameIdentifierOwner, NavigationItem`. `CompactNamedElementImpl.getNameIdentifier()` = first `IDENTIFIER` child; `getName()`/`getTextOffset()` derived; **`setName()` throws `IncorrectOperationException("Rename is not implemented yet")`**. Named impls exist for module/struct/enum/external-contract/type-alias/ledger/witness/circuit only.

**`CompactReferenceExprImpl`** exists but is an **empty stub** — no `getReference()`.

**Parser ↔ PSI contract confirmed by reading `parser/CompactParser.java`:**
- `REFERENCE_EXPR` = a single `IDENTIFIER` leaf (value reference site).
- `TYPE_REFERENCE` = leading `IDENTIFIER` (type name) + optional `GENERIC_ARGUMENT_LIST`.
- `MEMBER_EXPR` = `base-expr DOT IDENTIFIER`; `CALL_EXPR` for `base.method(...)`. Member/method name is a **leaf**, not a node.
- `STRUCT_LITERAL_EXPR` = `IDENTIFIER` (struct/type name) + `{ field: expr }` args (`STRUCT_ARG`).
- Circuit/witness parameters: `SIMPLE_PARAMETER_LIST` → `TYPED_ID` (`name : Type`), name = first `IDENTIFIER`.
- Constructor/lambda/arrow params + `const` bindings: `PATTERN`-based (`TYPED_PATTERN` / `OPTIONALLY_TYPED_PATTERN` / `CONST_BINDING`); simple identifier patterns nameable, destructuring not.
- `IMPORT_FORM`: bare module-name `IDENTIFIER` leaf, optional `IMPORT_SELECTION` (`{ IMPORT_ELEMENT, ... } from`), optional `IMPORT_PREFIX` (`prefix IDENTIFIER`). `IMPORT_ELEMENT` = `id [as id]`.
- No `::` operator anywhere — module members are reached by **prefix name-flattening** (`import M prefix $;` → members usable as `$name`).

### Discrepancies vs AGENTS.md
- AGENTS.md lists `CompactReferenceExpr` as prepared "for future references"; verified it is a bare stub with no `PsiReference` (accurate but worth reconfirming in the plan).
- AGENTS.md tables imply per-node reference ownership; verified several intended reference sites (`TYPE_REFERENCE`, `MEMBER_EXPR`, `STRUCT_LITERAL_EXPR`, `IMPORT_ELEMENT`) are currently generic `CompactPsiElement` and must gain dedicated impls.

# Semantics & Scope Model

### Compact Semantic Model (derived from `references/`, `compact/`, and `type-example.compact`)

Compact keeps **two lookup namespaces**:

- **Type namespace:** type aliases (`type`/`new type`, incl. generic `type X<#n> = ...`), `struct`, `enum`, `external contract`, in-scope generic parameters (`#N`), and prefixed/selected imported type names. (Builtins `Boolean/Bytes/Field/Opaque/Uint/Vector` and stdlib types like `Counter`, `Map`, `Either`, `ShieldedCoinInfo` are recognized but **soft-unresolved** for now.)
- **Value namespace:** `const` bindings, parameters, `for` loop variables, lambda params, `ledger` names, `circuit` names, `witness` names, `constructor`-scoped bindings, and prefixed/selected imported value names.

### Scope Hierarchy (single-file)

```
File scope (top-level program elements + prefixed/selected imports)
 └─ Module scope (module M { ... } — own program elements)
     └─ Circuit / Witness / Constructor scope (generic params + parameters)
         └─ Block scope (const bindings; nested blocks)
             └─ for-loop scope (loop `const i` visible in loop body)
Struct scope (field names) — used for struct-literal fields & (future) field access
Enum scope (member names) — used for `Enum.member`
```

**Lookup rules (verified against examples):**
- **Order:** innermost binding first; walk PSI parents outward to file, then imported names.
- **Shadowing:** inner `const`/parameter shadows outer/file-level of the same namespace.
- **Module members:** an exported module member `M.x` becomes a flat name `$x` after `import M prefix $;` (see `type-example.compact` Test 20: `import M20 prefix $;` then `$test()`, `$vara`, type `$t20_boolean`). Selection `import { a as b } from M;` introduces name `b` bound to `M`'s `a`.
- **`export { name, ... };`** references already-declared or imported names (may be prefixed, e.g. `$test`).
- **Enum member access:** `T14.t14_a` — base `REFERENCE_EXPR` resolves to an `enum`; member resolves within that enum's `ENUM_MEMBER`s. This does **not** need type inference.
- **Struct-literal fields:** `T4a { a: ... }` — `a` resolves to the named struct's `STRUCT_FIELD`.
- **Type references** may be simple aliases, chained aliases (`new type t6_b = t6_a;`), generic (`t35_pair<4,8>`), or tuple types; each identifier in type position resolves in the type namespace.

### Explicit Open Questions (do not guess)
- Whether type and value namespaces are truly disjoint for identical names (examples don't collide; treat as separate but confirm against `compact/compiler/expand-modules-and-types.ss`).
- Exact visibility of non-`export`ed module members from outside the module (assume only `export`ed members are importable; confirm upstream).
- Ledger ADT method resolution (`t10_map.insert(...)`) and struct field access (`vara.a`) — require type inference; **deferred**.
- Cross-file `include`/import target resolution and stdlib symbol resolution — **deferred** (single-file first).

# Reference & Resolve Architecture

### Attachment Mechanism (confirmed decision)

References are attached by **overriding `getReference()` / `getReferences()` on the handwritten PSI impls** (Elixir-style), not via a central `PsiReferenceContributor`. This fits the existing handwritten PSI and avoids `ElementPattern` indirection over custom element types.

### New `resolve` package — scope-walking engine (no indexes)

`src/main/java/dev/verloren/midnight/resolve/`
- **`CompactResolveUtil`** — static entry points:
  - `resolveValue(String name, PsiElement place)` and `resolveType(String name, PsiElement place)` returning candidate `CompactNamedElement`(s).
  - `collectValueDeclarations(PsiElement place)` / `collectTypeDeclarations(PsiElement place)` — used by both references and completion.
  - `moduleExports(CompactModuleDefinition)`, `prefixImports(CompactFile)`, `selectionImports(CompactFile)` — model `prefix $` flattening and `{ a as b } from M` selections.
  - Walks parents via `PsiTreeUtil.getParentOfType`/`getChildrenOfType`; collects declarations per namespace; applies innermost-first shadowing.
- **`CompactScopeProcessor`** *(optional helper)* — a lightweight visitor that yields named elements in scope for a given `place` and namespace, so references and completion share one traversal.

### New `reference` package — `PsiReference` classes

`src/main/java/dev/verloren/midnight/reference/`
- **`CompactReferenceBase`** (abstract) extends `com.intellij.psi.PsiPolyVariantReferenceBase<PsiElement>`; implements `handleElementRename` (replace the name identifier leaf), `getVariants()` returns empty (completion handled by the contributor), and caches via `ResolveCache` (Rust `RsReferenceBase` pattern, simplified).
- **`CompactValueReference`** — for `REFERENCE_EXPR`; range = identifier; resolves in value namespace. Multi-target possible (duplicate declarations).
- **`CompactTypeReference`** — for `TYPE_REFERENCE`, `STRUCT_LITERAL_EXPR` name, and `IMPLEMENTS_DECLARATION` target; range = leading type-name identifier; resolves in type namespace; soft-unresolved for builtins/stdlib.
- **`CompactEnumMemberReference`** — for `MEMBER_EXPR` whose base resolves to an `enum`; resolves the trailing member identifier to an `ENUM_MEMBER`. Returns unresolved otherwise (no type inference).
- **`CompactImportReference`** — for `IMPORT_ELEMENT` source name → exported member of the referenced same-file module; module-name leaf in `IMPORT_FORM` → same-file `MODULE_DEFINITION`.

### Reference site summary

| Site (PSI)                        | Reference class              | Name element / range | Target                                                          | Multi? | Needs future type info |
|-----------------------------------|------------------------------|----------------------|-----------------------------------------------------------------|--------|------------------------|
| `REFERENCE_EXPR`                  | `CompactValueReference`      | identifier leaf      | value decl / param / local / prefixed value                     | yes    | no                     |
| `TYPE_REFERENCE`                  | `CompactTypeReference`       | leading identifier   | type alias/struct/enum/ext-contract/generic param/prefixed type | yes    | no                     |
| `STRUCT_LITERAL_EXPR` (name)      | `CompactTypeReference`       | leading identifier   | struct/type                                                     | yes    | no                     |
| `MEMBER_EXPR` (`Enum.m`)          | `CompactEnumMemberReference` | trailing identifier  | `ENUM_MEMBER`                                                   | no     | partial                |
| `MEMBER_EXPR` (`v.field`)         | — (deferred)                 | —                    | struct field / ADT method                                       | —      | **yes**                |
| `IMPORT_ELEMENT` / import name    | `CompactImportReference`     | identifier           | same-file module member/module                                  | no     | no                     |
| `IMPLEMENTS_DECLARATION` (target) | `CompactTypeReference`       | type identifier      | `CONTRACT_DECLARATION`                                          | no     | no                     |

### Go To Declaration / Resolve

No dedicated `GotoDeclarationHandler` — the platform's default action uses `PsiReference.resolve()`. Correct references ⇒ Go To Declaration works automatically. Ambiguity is surfaced through `PsiPolyVariantReference.multiResolve`; unresolved references simply return `null`/empty (no error highlighting is added in Phase 3).

# Features & File Plan

### Named-element wrappers (confirmed decision: add wrappers)

New PSI impls extending `CompactNamedElementImpl`, wired in `CompactElementFactory`:
- `psi/CompactParameterImpl` (`TYPED_ID`) — name = first `IDENTIFIER`.
- `psi/CompactStructFieldImpl` (`STRUCT_FIELD`) — name = first `IDENTIFIER`.
- `psi/CompactEnumMemberImpl` (`ENUM_MEMBER`) — name = the `IDENTIFIER`.
- `psi/CompactConstBindingImpl` (`CONST_BINDING`) — name = first `IDENTIFIER` of the pattern (simple patterns only; destructuring = documented limitation).
- `psi/CompactGenericParameterImpl` (`GENERIC_PARAMETER`) — name = `IDENTIFIER` after optional `#`.
- `psi/CompactImportElementImpl` (`IMPORT_ELEMENT`) — **custom** `getNameIdentifier()`: returns the alias (last identifier) when `as` present, else the single identifier; also owns `CompactImportReference` for the source name.

Constructor/lambda/arrow parameters are `PATTERN`-based; expose their identifier through a small `psi/CompactPatternImpl` (`PATTERN`) named-element for simple identifier patterns (destructuring deferred). For-loop `const i` variable is a leaf in `FOR_STATEMENT`; resolve/rename target the leaf and it is documented as a partial-support case (no parser change permitted).

### Rename
- Implement `CompactNamedElementImpl.setName(...)` to replace the name-identifier leaf using a helper `psi/CompactElementFactory.createIdentifierLeaf(project, text)` (parse a throwaway `CompactFile`, extract the identifier). 
- `CompactReferenceBase.handleElementRename` replaces the reference identifier text.
- `refactoring/CompactNamesValidator implements com.intellij.lang.refactoring.NamesValidator` — `isKeyword` via `CompactTokenSets.KEYWORDS`, `isIdentifier` via the lexer/identifier rule. Registered as `lang.namesValidator`.
- `refactoring/CompactRefactoringSupportProvider extends RefactoringSupportProvider` *(optional)* — enable in-place member rename. Registered as `lang.refactoringSupport`.

### Find Usages
- `findUsages/CompactFindUsagesProvider implements com.intellij.lang.findUsages.FindUsagesProvider`:
  - `getWordsScanner()` → `DefaultWordsScanner` over a fresh `CompactLexer` with identifier tokens, `CompactTokenSets.COMMENTS`, and `CompactTokenSets.LITERALS`.
  - `canFindUsagesFor(e)` → `e instanceof CompactNamedElement`.
  - `type`/`getDescriptiveName`/`getNodeText`/`getHelpId` classify declarations (circuit, witness, ledger, struct, enum, type, module, parameter, field, member, local, import). Registered as `lang.findUsagesProvider`.
- Relies on the Phase 3 references so `ReferencesSearch` maps words back to targets.

### Completion
- `completion/CompactCompletionContributor extends com.intellij.codeInsight.completion.CompletionContributor`, registered as `completion.contributor` (language Compact). Contexts classified by `completion/CompactCompletionContext` (inspects caret PSI parents/preceding tokens):
  - **Keyword context:** at statement/declaration start → relevant subset of `CompactTokenSets.KEYWORDS` (not a blanket dump).
  - **Type context** (after `:`, inside `<...>`, after `as`, `default<`, ledger/param/return type): `CompactTokenSets.BUILTIN_TYPES` + in-scope type decls (via `CompactResolveUtil.collectTypeDeclarations`) + prefixed/selected imported types.
  - **Value context:** in-scope value decls (locals, params, ledgers, circuits, witnesses, prefixed values) + value keywords (`true/false`, `default`, `disclose`, `map`, `fold`, `pad`, `slice`, `assert`, `emit`).
  - **Member context** after `.`: enum members when base resolves to an enum; struct fields deferred (type info).
- Optional per-context providers `completion/CompactKeyword|Type|ValueCompletionProvider` sharing `CompactResolveUtil`.

### File-by-file summary

**New files** (all Java, under `src/main/java/dev/verloren/midnight/`): `resolve/CompactResolveUtil`, `resolve/CompactScopeProcessor`(opt); `reference/CompactReferenceBase`, `reference/CompactValueReference`, `reference/CompactTypeReference`, `reference/CompactEnumMemberReference`, `reference/CompactImportReference`; `psi/CompactTypeReferenceImpl`, `psi/CompactMemberExprImpl`, `psi/CompactStructLiteralExprImpl`, `psi/CompactImportElementImpl`, `psi/CompactParameterImpl`, `psi/CompactStructFieldImpl`, `psi/CompactEnumMemberImpl`, `psi/CompactConstBindingImpl`, `psi/CompactGenericParameterImpl`, `psi/CompactPatternImpl`; `findUsages/CompactFindUsagesProvider`; `refactoring/CompactNamesValidator`, `refactoring/CompactRefactoringSupportProvider`(opt); `completion/CompactCompletionContributor`, `completion/CompactCompletionContext`, optional per-context providers.

**Modified files:**
- `psi/CompactElementFactory.java` — add cases + update `hasDedicatedElement` for the new impls.
- `psi/CompactReferenceExprImpl.java` — override `getReference()` → `CompactValueReference`.
- `psi/CompactNamedElementImpl.java` — implement `setName(...)`.
- `psi/CompactContractImplementsDeclarationImpl.java` — override `getReference()` → `CompactTypeReference`.
- `psi/CompactImportDeclarationImpl.java` / `psi/CompactModuleDefinitionImpl.java` — accessors for module name/prefix/selection/exported members used by the resolver.
- `psi/CompactFile.java` *(minor)* — `getModules()`/`getImports()` convenience if needed.
- `resources/META-INF/plugin.xml` — register `lang.findUsagesProvider`, `completion.contributor`, `lang.namesValidator`, optional `lang.refactoringSupport`.
- `AGENTS.md` — durable Phase 3 section (Output 2).
- `.junie/plans/phase-3-reference-resolution-completion.md` — the plan doc (Output 1).

**Must NOT be modified:** `parser/CompactParser.java`, `parser/CompactElementTypes.java`, `lexer/CompactLexer.java`, `lexer/CompactTokenTypes.java`, `grammar/*`, `Compact.flex`, `Reference.bnf`, and all tests. No new element types or tokens are needed.

**Reusable existing classes:** `CompactNamedElement`/`CompactNamedElementImpl`, `CompactTokenSets` (KEYWORDS/BUILTIN_TYPES/LITERALS/COMMENTS), `CompactFile.getDeclarations()`, existing declaration impls.

### IntelliJ reference-implementation mapping
- **Elixir** (`reference/Callable.kt`, `psi/scope/*`, `PsiNamedElementImpl.kt`, `find_usages/Provider.kt`): adopt scope-walking resolve, lightweight named elements, `DefaultWordsScanner` find-usages. This is the primary model.
- **Rust** (`RsReferenceBase.kt`, `RsNamedElement.kt`, `RsFindUsagesProvider.kt`, `RsCompletionContributor.kt`): adopt `ResolveCache`-backed reference base and `setName`/completion structure; **avoid** stub indexing, macro engine, and DFA/type-inference — over-engineered for single-file Compact.

# Testing & Risks

### Future Testing Requirements (specify only — do NOT implement now)

A later testing agent should add (in `src/test`, mirroring existing `ParsingTestCase`/light-fixture style):
- **Resolve:** local const, parameter, block-scope, shadowing (inner shadows outer), same-name duplicates (multi-resolve), separate type vs value namespaces.
- **Type resolution:** type alias, chained aliases, struct, enum, external contract, generic parameter; builtins/stdlib remain soft-unresolved (no false errors).
- **Member resolution:** `Enum.member` resolves; `v.field` remains unresolved (deferred).
- **Import/module:** `import M prefix $;` → `$name` resolves to module member; `import { a as b } from M;` → `b` resolves; `export { $name };` references resolve.
- **Go To Declaration:** navigation lands on the correct name identifier for each site above.
- **Find Usages:** all supported `CompactNamedElement` kinds; word-scanner picks up identifiers, ignores comments/literals.
- **Rename:** declaration + all in-file references updated; alias vs source-name in `IMPORT_ELEMENT`; keyword/invalid-name rejection via `NamesValidator`; prefixed references updated correctly.
- **Completion contexts:** keyword-at-start, type-position, value-position, `Enum.` member; assert no blanket keyword dump in type/value positions.

### Risks & Mitigations
- **Missing type inference** limits `v.field`/ADT method resolution → explicitly deferred; enum-member resolution handled without inference.
- **Prefix flattening** (`$name`) parsing depends on `$` being part of identifiers (confirmed in lexer notes) → resolver must strip/match the prefix from `IMPORT_PREFIX`; add targeted checks.
- **Destructuring patterns** yield multiple names — only simple identifier patterns supported in Phase 3; documented limitation.
- **`setName` leaf replacement** must not break incremental reparse → use a parsed throwaway `CompactFile` to obtain a valid `IDENTIFIER` leaf.
- **Cross-IDE coupling** — audit imports to stay within `com.intellij.psi/lang/codeInsight`; flag any IDEA-only API.
- **Resolve performance** — single-file `PsiTreeUtil` walking is cheap; wrap in `ResolveCache` to avoid repeated walks.

### Open Questions (carried into the plan doc & AGENTS.md)
1. Are type and value namespaces fully disjoint for identical names? (confirm via `expand-modules-and-types.ss`)
2. Are non-`export`ed module members ever importable?
3. Exact `include "file";` semantics and target resolution across files (deferred).
4. Stdlib symbol surface for resolution/completion (deferred).
5. For-loop variable and destructuring bindings as first-class named elements without parser changes.

### Validation Checkpoints
- After foundation: project compiles; `setName`/resolver util unit-testable in isolation; no EP regressions.
- After references: Ctrl-Click resolves value/type/enum-member/import sites in `type-example.compact`.
- After find-usages/rename: usages listed and rename updates references in-file.
- After completion: contextual suggestions verified per context; no blanket dumps.

# Delivery Steps

###   Step 1: Persist Phase 3 plan doc and update AGENTS.md
The Phase 3 architecture is captured durably in the repo without touching any production/test code.

- Create `.junie/plans/phase-3-reference-resolution-completion.md` containing all 16 required sections (verified current state, semantic model, scope hierarchy, reference architecture, resolve architecture, Go To Declaration, Find Usages, Rename, Completion, IntelliJ decisions, file-by-file changes, implementation order, future testing requirements, risks, open questions, validation checkpoints) — sourced from the proposal tabs.
- Add an `## Phase 3 Architecture — References, Resolution & Completion` section to `AGENTS.md` with durable findings only: confirmed scope/namespace rules, prefix name-flattening (`$name`), reference-owning PSI nodes, chosen IntelliJ APIs, key Rust/Elixir patterns, confirmed limitations, and open questions.
- Do not copy the full plan into `AGENTS.md`; keep it a durable index pointing to the plan file.

###   Step 2: Build resolve foundation and named-element wrappers
A single-file scope-walking resolver exists and all declaration nodes are renameable named elements.

- Add `resolve/CompactResolveUtil` (+ optional `CompactScopeProcessor`) implementing `resolveValue`/`resolveType`, `collectValue/TypeDeclarations`, and module `prefix`/selection import modeling via `PsiTreeUtil` scope walking with innermost-first shadowing and separate type/value namespaces.
- Add named-element impls extending `CompactNamedElementImpl`: `CompactParameterImpl` (`TYPED_ID`), `CompactStructFieldImpl`, `CompactEnumMemberImpl`, `CompactConstBindingImpl`, `CompactGenericParameterImpl`, `CompactPatternImpl`, and `CompactImportElementImpl` (custom alias-aware `getNameIdentifier`).
- Implement `CompactNamedElementImpl.setName(...)` using a new `CompactElementFactory.createIdentifierLeaf(...)` helper.
- Wire all new impls in `CompactElementFactory.createElement`/`hasDedicatedElement`.
- Add module/import accessors on `CompactModuleDefinitionImpl`/`CompactImportDeclarationImpl` for the resolver.

###   Step 3: Implement PsiReferences and Go To Declaration
Value, type, enum-member and import references resolve, enabling Go To Declaration with no dedicated handler.

- Add `reference/CompactReferenceBase` (abstract, `PsiPolyVariantReferenceBase` + `ResolveCache` + `handleElementRename`).
- Add `CompactValueReference`, `CompactTypeReference`, `CompactEnumMemberReference`, `CompactImportReference` delegating to `CompactResolveUtil`.
- Override `getReference()` on `CompactReferenceExprImpl` (value), new `CompactTypeReferenceImpl` (`TYPE_REFERENCE`), `CompactStructLiteralExprImpl`, `CompactMemberExprImpl` (enum member), `CompactImportElementImpl`, and `CompactContractImplementsDeclarationImpl` (implements target).
- Ensure builtins/stdlib return soft-unresolved (no exceptions), and multi-target duplicates resolve via `multiResolve`.

###   Step 4: Implement Find Usages and Rename wiring
Usages of any Compact declaration are found and rename updates the declaration plus in-file references safely.

- Add `findUsages/CompactFindUsagesProvider` with a `DefaultWordsScanner` over `CompactLexer` (identifiers + `CompactTokenSets.COMMENTS`/`LITERALS`), `canFindUsagesFor` on `CompactNamedElement`, and type/descriptive-name classification.
- Add `refactoring/CompactNamesValidator` (keywords via `CompactTokenSets.KEYWORDS`, identifier rule via lexer) and optional `refactoring/CompactRefactoringSupportProvider` for in-place rename.
- Verify rename flows through `setName` + reference `handleElementRename`, including `IMPORT_ELEMENT` alias vs source name and prefixed (`$name`) references.
- Register `lang.findUsagesProvider`, `lang.namesValidator`, and optional `lang.refactoringSupport` in `plugin.xml`.

###   Step 5: Implement contextual completion
Completion suggests the right category based on caret PSI context rather than dumping all keywords.

- Add `completion/CompactCompletionContext` to classify caret position (keyword-start, type position, value position, member-after-dot).
- Add `completion/CompactCompletionContributor` (+ optional per-context providers) producing: contextual keyword subsets; builtin types + in-scope/imported type declarations in type positions; in-scope values + value keywords in expression positions; enum members after `Enum.`.
- Reuse `CompactResolveUtil.collectValue/TypeDeclarations` and `CompactTokenSets` for candidate sources.
- Register `completion.contributor` for language Compact in `plugin.xml`; leave struct field-after-dot completion as a documented deferred case.