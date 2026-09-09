# ADR-002: Multi-File Symbol Resolution & Split Namespace Architecture

## Status
Accepted (Phase 4–6 / Complete)

## Context & Problem Statement
Smart contract development requires instant symbol navigation (Go to Declaration, Find Usages, Rename Refactoring) and scope-aware autocompletion across files.
Compact introduces specific semantic scoping challenges:
1. **Namespace Disambiguation**: Compact has strictly separate namespaces for values and types. An identifier `Point` in expression context (`const p = Point;`) must never resolve to `struct Point` (type), and `Point` in a type annotation (`x: Point`) must never resolve to `const Point` (value).
2. **Lexical Shadowing**: Inner block variables (`const x`) must cleanly shadow outer variables without obliterating the outer symbol in enclosing scopes.
3. **Cross-File Scoping**: Compact contracts use `include "filename.compact";` and `import { ... } from "module";` to compose contracts. Circular includes must not crash the IDE or cause infinite recursion.
4. **Visibility Boundaries**: Declarations inside modules or contracts must obey `export` boundaries.

## Authoritative References
1. **Official Midnight Compiler Scoping**:
   - [`compact/compiler/parser.ss`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/compact/compiler/parser.ss): Production rules for `include`, `import`, `export`, and block-level variable bindings (`const`, `circuit`, `witness`, `ledger`).
   - [`compact/compiler/langs.ss`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/compact/compiler/langs.ss): Scoping semantics separating type expressions (`Type`) from value expressions (`Expr`).
2. **Production JetBrains Implementations**:
   - `intellij-rust`: [`RsResolveProcessor.kt`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/intellij-rust/src/main/kotlin/org/rust/lang/core/resolve/RsResolveProcessor.kt) — implements dual namespace resolution (`Namespace.Types`, `Namespace.Values`) and scoped lexical walking.

## Decision
1. **Strict Namespace Separation (`CompactResolveUtil.Namespace`)**:
   Implement distinct `Namespace.VALUE` and `Namespace.TYPE` resolution pathways. All symbol resolution calls must explicitly declare the intended namespace (`resolveValue`, `resolveType`).
2. **Innermost Lexical Scope Walking**:
   Resolution begins at the reference site and traverses outward:
   - Block scopes (loops, conditionals, circuit bodies) with **forward-reference safety** (declarations after the caret offset in the same block are not visible).
   - Containing circuit / constructor parameters.
   - Contract and module member declarations.
   - File-level declarations.
   - Cross-file `include` and `import` declarations.
   - Built-in standard library symbols.
3. **Cycle-Safe Recursive Cross-File Include Resolution**:
   `CompactResolveUtil` traverses relative file inclusions via IntelliJ's Virtual File System (`VfsUtil`). Cycle detection uses a `Set<PsiFile> visited` guard to terminate recursion instantly when cyclic includes are encountered.
4. **Module Export Verification**:
   Symbols originating in other files or external modules are filtered against `CompactExportStatement` or `isExported()` checks.

## Scalability & Anti-Hardcoding Evaluation
- **Is it hardcoded?**: No. Scoping follows the hierarchical PSI tree structure (`PsiTreeUtil.getParentOfType`) and lexical offset checks rather than hardcoded name mappings.
- **Is it scalable?**: Yes. Any new declaration construct (e.g. interfaces, enum variants) automatically plugs into the `CompactNamedElement` hierarchy and obeys the exact same scoping rules.

## Feature Implementation Map
- Resolution Engine: [`CompactResolveUtil.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/resolve/CompactResolveUtil.java)
- Scope Definitions: [`CompactScope.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/scope/CompactScope.java), [`CompactScopes.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/scope/CompactScopes.java)
- Symbol Models: [`CompactSymbol.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/symbol/CompactSymbol.java), [`CompactSymbols.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/symbol/CompactSymbols.java)
- Reference Impls:
  - [`CompactReferenceExprImpl.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/psi/impl/CompactReferenceExprImpl.java)
  - [`CompactTypeReferenceImpl.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/psi/impl/CompactTypeReferenceImpl.java)
- Test Suites:
  - [`CompactReferenceTest.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/reference/CompactReferenceTest.java)
  - [`CompactCompletionTest.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/completion/CompactCompletionTest.java)

## Consequences & Future Maintenance
- Maintain strict separation of `VALUE` and `TYPE` namespaces. Never merge them in inspections or completion without an explicit dual-pass.
- Large workspace indexing can build on top of this model via `StubIndex` in Phase 30 without changing the `CompactNamedElement` interface.
