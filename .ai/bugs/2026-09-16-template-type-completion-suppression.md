# Live Template and Declaration Type Position Completion Suppression

- **Date:** 2026-09-16
- **Feature / Component:** completion / live-templates
- **Severity:** High
- **Status:** Resolved

## Symptoms

When inserting scaffolding declarations or live templates (e.g., `expled`, `led`, `cir`, `wit`, `type`, `const`), the live template expands properly into an interactive template session. However, upon tabbing into the data type field (for example, replacing `State` with `Bytes` or `Field` in `export ledger foo: State;`), no completion popup appeared, and typing trigger characters did not offer data types. Furthermore, in existing code, typing in a declaration type position after `export` offered only exportable declaration keywords rather than type names.

## Context

User requested fixing live templates (`expled`, `led`, `cir`, `wit`, `type`, etc.) where interactive parameter replacement allows changing identifier names and types, but failed to offer type completions (e.g. from `State` to `Bytes`).

## Root Cause

Three distinct root causes combined to suppress type completion in template and declaration editing contexts:

1. **Static Live Template Expression Nodes:**
   In `CompactLedgerInsertHandler` and `CompactDeclarationInsertHandler`, live template variable nodes were constructed using `new ConstantNode("State")` or `new ConstantNode("Void")`. `ConstantNode` implements `Expression` with a `calculateLookupItems(...)` returning `null`. In IntelliJ IDEA's `TemplateState`, lookup suggestions are only automatically provided if the variable's `Expression` overrides `calculateLookupItems` to provide `LookupElement[]`.
   Similarly, live template XML definitions (`Compact.xml`) lacked macro expressions that return lookup items for type variables.

2. **Aggressive `isExportPreceding` Heuristic Overriding Declaration Types:**
   In `CompactCompletionContributor`, an early-return check:
   ```java
   if (CompactCompletionContext.isExportPreceding(position)) {
     addAfterExportCompletions(result);
     return;
   }
   ```
   was triggered whenever `export` appeared earlier on the line. Because `isExportPreceding` walked backward across colons, equals signs, and parentheses without boundary stop conditions, any cursor position following `export` (even after `ledger name: `) was misclassified as `Kind.AFTER_EXPORT`, suppressing all type completions and suggesting keywords (`circuit`, `ledger`, `struct`, etc.) instead.

3. **Incomplete Built-in Types List:**
   `CompactCompletionContributor.BUILTIN_TYPES` omitted several fundamental Compact types including `State`, `Counter`, `Void`, `JubjubPoint`, and `Secp256k1Point`.

## Investigation

1. Inspected `CompactLedgerInsertHandler.java` and `CompactDeclarationInsertHandler.java`: found `builder.addVariable("TYPE", new ConstantNode("State"), true);`. Since `ConstantNode` provides no lookup elements, IntelliJ template manager does not offer completion when navigating to that variable.
2. Created a dedicated `CompactTypeExpression` subclassing `Expression` and overriding `calculateLookupItems(ExpressionContext context)` to gather `CompactCompletionContributor.BUILTIN_TYPES`, in-scope types via `CompactResolveUtil.collectTypeDeclarations`, and imported types via `CompactResolveUtil.prefixedImportNames`.
3. Created `CompactTypeMacro` implementing `Macro` to make the type expression usable in XML live templates via `compactType("State")`.
4. Discovered that even with manual completion (`Ctrl+Space`) inside `export ledger foo: <caret>;`, `CompactCompletionContributor.addCompactCompletions` bypassed classification entirely due to `if (CompactCompletionContext.isExportPreceding(position))`.
5. Hardened `CompactCompletionContext`:
   - Stopped backward traversal in `isExportPreceding` at statement boundaries, colons, assignments, parentheses, and declaration keywords.
   - Added `isTypePosition` to recognize cursor slots after colons in ledger declarations, struct fields, const bindings, and after `=` in type aliases.
   - Expanded `BUILTIN_TYPES` to include `State`, `Counter`, `Void`, `JubjubPoint`, and `Secp256k1Point`.

## Solution

1. **Created `CompactTypeExpression.java`:**
   Implements `Expression` for template builders. Overrides `calculateLookupItems` to populate completion items with all built-in types, local types from PSI, and imported types.
2. **Created `CompactTypeMacro.java` & Registered in `plugin.xml`:**
   Provides `compactType(defaultType)` macro for IntelliJ live template XML files.
3. **Updated Live Templates in `Compact.xml`:**
   Updated `cct`, `cir`, `wit`, `expw`, `led`, `ledg`, `ledger`, `type`, `expt`, `const`, and added `expled`, configuring type variables with `compactType(...)`.
4. **Updated `CompactLedgerInsertHandler` & `CompactDeclarationInsertHandler`:**
   Replaced `ConstantNode` with `new CompactTypeExpression(...)` for all type variable slots.
5. **Hardened Context Classification in `CompactCompletionContext` & `CompactCompletionContributor`:**
   - Removed rogue early return in `CompactCompletionContributor`.
   - Hardened `isExportPreceding` backward traversal and regex.
   - Added `isTypePosition` check returning `Kind.TYPE`.
   - Added `State`, `Counter`, `Void`, `JubjubPoint`, `Secp256k1Point` to `BUILTIN_TYPES`.

## Verification

Added unit tests in `CompactCompletionTest.java`:
- `testExportLedgerTypeCompletion`: Verifies `State`, `Bytes`, `Field` are suggested in `export ledger ledger1: <caret>;` and keywords are excluded.
- `testExportLedgerPrefixTypeCompletion`: Verifies typing `St` completes `State`.
- `testBareLedgerTypeCompletion`: Verifies `State` and `Counter` are suggested in `ledger myLedger: <caret>;`.
- `testExportCircuitReturnTypeCompletion`: Verifies `Void`, `Field`, `Boolean` are suggested for circuit return types.
- `testStructFieldTypeCompletion`: Verifies type completion in struct field definitions.
- `testTypeAliasTypeCompletion`: Verifies type completion in type alias definitions.
- `testTypeExpressionLookupItems`: Verifies `CompactTypeExpression.calculateLookupItems` returns built-ins and custom declared structs.
- `testTypeMacroDelegatesToExpression`: Verifies macro execution and result calculation.

Ran full test suite: `./gradlew test` -> 561 tests executed, 0 failures.

## Prevention / Lesson

When generating template variables that represent types or identifiers with finite or predictable candidates, never use raw `ConstantNode`. Always provide an `Expression` or `Macro` that returns `calculateLookupItems(...)` so that IntelliJ's live template engine automatically activates the lookup list upon navigation.
Furthermore, avoid top-level bypass checks in completion contributors that supersede AST context classification.

## Related Files

- `src/main/java/dev/verloren/midnight/ide/templates/CompactTypeExpression.java`
- `src/main/java/dev/verloren/midnight/ide/templates/CompactTypeMacro.java`
- `src/main/java/dev/verloren/midnight/completion/CompactLedgerInsertHandler.java`
- `src/main/java/dev/verloren/midnight/completion/CompactDeclarationInsertHandler.java`
- `src/main/java/dev/verloren/midnight/completion/CompactCompletionContributor.java`
- `src/main/java/dev/verloren/midnight/completion/CompactCompletionContext.java`
- `src/main/resources/liveTemplates/Compact.xml`
- `src/main/resources/META-INF/plugin.xml`
- `src/test/java/dev/verloren/midnight/completion/CompactCompletionTest.java`

## Related ADRs / Context

- None
