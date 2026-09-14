# ADR-028: Prohibit Top-Level Export Const & Strict Compact Grammar Alignment

## Status
Accepted (v1.2.6 / Complete)

## Context & Problem Statement
During review and cross-verification of top-level export declarations against the upstream Compact compiler parser grammar (`compact/compiler/parser.ss`), the syntax validity of `export const` was investigated.

### Upstream Compiler Grammar Findings
In `compact/compiler/parser.ss`:
1. **`Program-element` (Top-Level Declarations)**:
   ```scheme
   (define-syntax Program-element
     (syntax-rules ()
       [(_ ...)
        (or (pragma-form ...)
            (module-definition ...)
            (import-form ...)
            (export-form ...)
            (include-form ...)
            (struct-declaration ...)
            (enum-declaration ...)
            (contract-declaration ...)
            (implements-declaration ...)
            (type-alias-declaration ...)
            (ledger-declaration ...)
            (witness-declaration ...)
            (constructor-definition ...)
            (circuit-definition ...))]))
   ```
   `const` is **absent** from `Program-element`.
2. **`Statement0` (Block Statements)**:
   `statement-const` is exclusively part of `Statement0 (stmt0)`, which only appears inside `Block (block)` (`{ ... }`).
3. **`export` Grammar**:
   `export` is allowed as a standalone export form (`export { id, ... };`) or as a prefix on top-level program elements (`ledger`, `circuit`, `witness`, `struct`, `enum`, `contract`, `type`, `module`).
   It is **never** syntactically valid on local statements (`const`, `if`, `for`, `return`, `assert`, `emit`).
4. **Canonical Constant Idiom in Compact**:
   In Compact modules and standard libraries, top-level constants are exported using `pure circuit`:
   ```compact
   export pure circuit UINT128_MAX(): Uint<128> {
     return 340282366920938463463374607431768211455;
   }
   ```

Prior to this alignment:
- Autocompletion erroneously suggested `export const` and `const` at the top level and after `export`.
- Live template `expconst` generated `export const $NAME$: $TYPE$ = $VALUE$;` at file scope.
- `CompactToggleExportIntention` offered to add/remove `export` on `const` statements.

## Authoritative References
- **Compact Compiler Parser**: [`compact/compiler/parser.ss`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/compact/compiler/parser.ss) — `Program-element` (lines 240–270) and `Statement0` (lines 460–480).
- **Compact Standard Library**: [`module.compact`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/module.compact) — Demonstrates `export pure circuit` pattern for module-level constants.

## Decision
1. **Autocompletion Filtering (`CompactCompletionContributor.java`)**:
   - Removed `export const` and bare `const` from top-level declaration completions (`addDeclarationCompletions`).
   - Removed `const` from completions following the `export` keyword (`addAfterExportCompletions`).
   - Retained `const` exclusively within `Kind.STATEMENT` (inside circuit and constructor bodies).
2. **Live Templates (`Compact.xml`, `MyMessageBundle.properties`)**:
   - Removed `expconst` live template.
   - Retained `const` live template exclusively for block statement contexts.
3. **Declaration Extensibility Registry (`CompactDeclarationType.java`)**:
   - Added `public boolean isExportable() { return this != CONST; }` to explicitly reflect that `const` is block-scoped.
4. **Intention Scoping (`CompactToggleExportIntention.java`)**:
   - Removed `CompactElementTypes.CONST_STATEMENT` matching so `Alt+Enter` never offers export toggling on `const` statements.
5. **Unit Test Verification**:
   - Updated `CompactCompletionTest.java` to assert `assertFalse` for `const` after `export` and for `export const` at top level.
   - Added `testExportConstNotSuggestedAtTopLevel`.
   - Updated `CompactPhase28IntentionsTest.java` to verify `testToggleExportNotAvailableOnConst`.

## Consequences & Maintenance
- Prevents generating or suggesting illegal syntax that would fail compiler verification.
- Enforces strict alignment with upstream Compact compiler specification.
