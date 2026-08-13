# Phase 4A - Semantic Model + Symbols/Scopes

## 1. Verified current architecture

- The active implementation is handwritten Java: `CompactLexer`, `CompactParser`, `CompactElementTypes`, handwritten PSI wrappers, and `CompactParserDefinition`.
- Phase 3 references are already implemented on PSI wrappers. `CompactReferenceBase` uses IntelliJ `ResolveCache` and returns PSI declarations through `ResolveResult`.
- `CompactResolveUtil` is PSI-based and single-file. It has `VALUE` and `TYPE` namespaces, innermost-first local lookup, module export lookup, selected import lookup, and prefix import flattening.
- Completion and Find Usages consume `CompactResolveUtil` and `CompactNamedElement`, not a symbol model.
- Phase 4B type infrastructure already exists: `CompactType`, `CompactPrimitiveType`, `CompactTypeElement`, `CompactExpression`, expression `getType()`, declaration `getType()` overrides, and `CompactStructFieldReference`.
- AGENTS.md says type inference is implemented and semantic model is planned. The user request correctly identifies that a symbol/scope layer is still missing.

## 2. Verified Compact semantic rules

- `references/compact-grammar.mdx` and `references/parser.ss` define top-level program elements: module, import, export, include, struct, enum, contract, implements, type alias, ledger, witness, constructor, and circuit.
- `references/expand-modules-and-types.ss` documents the compiler semantic pass: environments are rib chains, duplicate bindings are errors in the same scope, and lookup walks inward to outward.
- Compiler `Info` categories include modules, functions/circuits/witnesses, contracts, enums, structs, type aliases, ledgers, runtime variables, type-valued generic parameters, size-valued generic parameters, and import aliases/fixups.
- The current plugin PSI does not fully separate generic size parameters from type parameters and does not model compiler expansion IDs, overload candidates, or cross-file include/import semantics.
- Export visibility is syntactic in current PSI: direct `export` on a declaration or membership in an `export { ... }` form inside a module.

## 3. Symbol hierarchy

- `CompactSymbol`: shared semantic view over an origin PSI declaration.
- `CompactValueSymbol`: values and callable values such as circuits, witnesses, ledgers, parameters, locals/patterns, enum members in expression contexts, struct fields, and selected imports that resolve to values.
- `CompactTypeSymbol`: type-space declarations such as structs, enums, type aliases, external contracts, generic parameters, builtin types, and selected imports that resolve to types.
- `CompactModuleSymbol`: module declarations.
- Supporting enums:
  - `CompactSymbolKind`: exact declaration kind for UI/inspection use.
  - `CompactSymbolNamespace`: `VALUE`, `TYPE`, `MODULE`, `UNKNOWN`.
  - `CompactVisibility`: `LOCAL`, `MODULE`, `EXPORTED`, `BUILTIN`, `UNKNOWN`.

## 4. Scope architecture

- `CompactScope`: lightweight PSI-backed query object.
- `CompactPsiScope`: implementation for file/module/local/type/member scopes.
- `CompactScopeKind`: `FILE`, `MODULE`, `BLOCK`, `CALLABLE`, `CONSTRUCTOR`, `LAMBDA`, `FOR`, `TYPE_DECLARATION`, `STRUCT`, `ENUM`, `CONTRACT`, `UNKNOWN`.
- `CompactScopes`: utility factory/collector. It finds the nearest semantic scope for a PSI place and exposes visible symbols by delegating to existing `CompactResolveUtil`.
- Scope lookup remains editor-tolerant: unresolved, malformed, and incomplete PSI return empty collections or unknown symbols.

## 5. PSI <-> Symbol relationship

- Symbols do not replace PSI. They wrap or point to a `CompactNamedElement` origin.
- PSI remains authoritative for rename, navigation, use scope, and reference identity.
- Symbols expose semantic classification, visibility, containing module/symbol, qualified name, and type by delegating to PSI/type APIs.
- Builtin types may be represented as symbols without a PSI origin.

## 6. Resolution integration

- Existing `resolveValue`, `resolveType`, `collectValueDeclarations`, and `collectTypeDeclarations` must keep returning PSI declarations to avoid breaking Phase 3.
- Add symbol-oriented methods to `CompactResolveUtil`, such as `resolveValueSymbols`, `resolveTypeSymbols`, `collectValueSymbols`, `collectTypeSymbols`, and `scopeFor`.
- Reference implementations remain unchanged unless a compile issue requires imports.
- Symbol conversion occurs after PSI resolution: reference -> resolve -> PSI declaration -> symbol facade.

## 7. Type-system integration

- `CompactSymbol.getType()` delegates to `CompactTypeElement.getType()` when an origin exists.
- Unknown or malformed symbols return `CompactPrimitiveType.UNKNOWN`.
- Builtin type symbols return the existing primitive type where available.
- No new type inference rules or type hierarchy redesign are part of Phase 4A.

## 8. Module/contract relationships

- Modules are symbols and scopes because the grammar and compiler model them as named environments.
- Contracts are type symbols in current resolution, matching `CompactResolveUtil` and compiler `Info-contract` as a type-like external contract declaration.
- Circuits and witnesses are value symbols. Constructor has a scope but is not named in current PSI, so it is not a symbol.
- Import declarations are not symbols unless represented by selected import aliases (`CompactImportElementImpl`).
- Includes remain outside Phase 4A because current resolution is intentionally single-file.

## 9. Performance considerations

- Avoid global indexes and stubs.
- Reuse `ResolveCache` at the reference layer and PSI tree walking already present in `CompactResolveUtil`.
- Keep symbol instances cheap and immutable wrappers; do not persist them across PSI changes.
- Do not full-file scan unless the caller requests all visible symbols for a scope/completion-like operation.
- All APIs tolerate null names and invalid PSI.

## 10. Exact files to create

- `src/main/java/dev/verloren/midnight/symbol/CompactSymbol.java` (Java interface): common symbol API; depends on PSI and `CompactType`.
- `src/main/java/dev/verloren/midnight/symbol/CompactValueSymbol.java` (Java interface): marker/refinement for value symbols; depends on `CompactSymbol`.
- `src/main/java/dev/verloren/midnight/symbol/CompactTypeSymbol.java` (Java interface): marker/refinement for type symbols; depends on `CompactSymbol`.
- `src/main/java/dev/verloren/midnight/symbol/CompactModuleSymbol.java` (Java interface): marker/refinement for module symbols; depends on `CompactSymbol`.
- `src/main/java/dev/verloren/midnight/symbol/CompactSymbolKind.java` (Java enum): declaration-kind taxonomy; no external dependencies.
- `src/main/java/dev/verloren/midnight/symbol/CompactSymbolNamespace.java` (Java enum): semantic namespace; no external dependencies.
- `src/main/java/dev/verloren/midnight/symbol/CompactVisibility.java` (Java enum): symbol visibility; no external dependencies.
- `src/main/java/dev/verloren/midnight/symbol/CompactPsiSymbol.java` (Java class): PSI-backed symbol implementation and nested concrete value/type/module classes; depends on PSI, resolve utilities, and type APIs.
- `src/main/java/dev/verloren/midnight/symbol/CompactBuiltinTypeSymbol.java` (Java class): symbol for builtin type names; depends on `CompactType`.
- `src/main/java/dev/verloren/midnight/symbol/CompactSymbols.java` (Java utility): classify PSI declarations and create symbols; depends on PSI classes, token types, and resolve utility.
- `src/main/java/dev/verloren/midnight/scope/CompactScope.java` (Java interface): scope query API; depends on PSI and symbol classes.
- `src/main/java/dev/verloren/midnight/scope/CompactScopeKind.java` (Java enum): scope-kind taxonomy.
- `src/main/java/dev/verloren/midnight/scope/CompactPsiScope.java` (Java class): PSI-backed scope implementation; depends on `CompactResolveUtil` and symbols.
- `src/main/java/dev/verloren/midnight/scope/CompactScopes.java` (Java utility): find nearest scope and visible symbols; depends on PSI, element types, and `CompactResolveUtil`.
- `src/test/java/dev/verloren/midnight/symbol/CompactSymbolTest.java` (Java test): focused tests for symbol classification, type delegation, and scope lookup; depends on existing test fixture style.

## 11. Exact files to modify

- `src/main/java/dev/verloren/midnight/resolve/CompactResolveUtil.java`: add symbol collection/resolve helpers and expose `scopeFor`; reason: integrate Phase 4A without replacing PSI resolution; depends on new symbol/scope packages.
- `AGENTS.md`: add durable Phase 4A findings after implementation; reason: required permanent engineering notebook update.

## 12. Implementation order

1. Create symbol enums and interfaces.
2. Implement PSI-backed and builtin symbols.
3. Implement symbol classifier/factory.
4. Create scope enum/interface/implementation/utilities.
5. Add non-breaking symbol methods to `CompactResolveUtil`.
6. Add focused tests only for new semantic-model behavior.
7. Run relevant tests and `./gradlew.bat build`.
8. Update AGENTS.md with durable implementation notes.

## 13. Risks

- Current `CompactResolveUtil.isInNamespace` is private and has approximations; symbol classification must stay consistent with it.
- Import aliases can resolve to either value or type declarations, so symbol namespace may be derived from the target when available.
- Builtin types are not real PSI declarations, so symbol consumers must handle null origin PSI.
- The current type system represents nominal types with `CompactPrimitiveType(name)`, which is sufficient for Phase 4A but not a full semantic type model.

## 14. Open questions

- Exact compiler rule for same-name value/type declarations in one scope remains unresolved for IDE diagnostics.
- Cross-file include/import visibility is not implemented.
- Standard library symbol surface is not indexed.
- Generic size parameters versus type-valued parameters are not fully represented in current PSI.
- Function/circuit overload compatibility is compiler-level and not represented in Phase 4A.
- Non-exported module member visibility through imports should be validated against compiler behavior before inspections depend on it.

## 15. Validation checkpoints

- Existing parser/reference/completion/rename/find-usages tests continue to pass.
- New symbol tests confirm PSI-to-symbol classification.
- New scope tests confirm visible value/type/module symbols are exposed without changing reference resolution behavior.
- `./gradlew.bat build` passes.
