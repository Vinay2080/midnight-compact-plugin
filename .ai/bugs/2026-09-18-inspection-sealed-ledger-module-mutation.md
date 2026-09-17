# False-Positive Sealed Ledger Mutation Warning Inside Modules

- **Date:** 2026-09-18
- **Feature / Component:** inspection / sealed ledger / modules
- **Severity:** High
- **Status:** Resolved

## Symptoms

In `.compact` files, valid library modules such as OpenZeppelin Compact Contracts (`ShieldedAccessControl.compact`, `FungibleToken.compact`, `ConfidentialFungibleToken.compact`) reported a false-positive warning in the IntelliJ editor and inspection suite:

```
Cannot modify sealed ledger field '_instanceSalt' outside constructor
```

Specifically, in `contracts/src/access/ShieldedAccessControl.compact:271`:
```compact
module ShieldedAccessControl {
  ...
  export sealed ledger _instanceSalt: Bytes<32>;
  ...
  export circuit initialize(instanceSalt: Bytes<32>): [] {
    assert(instanceSalt != default<Bytes<32>>, "ShieldedAccessControl: Instance salt must not be 0");
    assertNotInitialized();
    _isInitialized = true;

    _instanceSalt = disclose(instanceSalt);
  }
}
```

## Context

In the Compact language, a `sealed ledger` field represents persistent on-chain state that is immutable after contract deployment. The official compiler (`check-sealed-fields.ss`) verifies that no exported circuit reachable after deployment can mutate a sealed field.

In top-level contracts, sealed fields are initialized directly in the `constructor(...)`. However, in Compact, library `module` declarations (`module ModuleName { ... }`) cannot declare constructors—constructors are grammatically and semantically restricted to top-level contract declarations. Reusable libraries like OpenZeppelin Compact Contracts encapsulate their initialization in exported circuits (such as `initialize`) intended to be called exclusively from the importing top-level contract's `constructor`.

## Root Cause

`CompactSealedFieldMutationInspection.java:checkAssignment` only checked whether an assignment to a sealed ledger was enclosed in a `CompactConstructorDeclaration`:
```java
if (PsiTreeUtil.getParentOfType(assignExpr, CompactConstructorDeclaration.class) != null) {
  return;
}
```
Because `module` declarations cannot have constructors, any sealed ledger initialization within a module circuit was erroneously flagged as modifying a sealed field outside a constructor.

## Investigation

Tracing against upstream compiler ground truth (`check-sealed-fields.ss`):
```scheme
(define-pass check-sealed-fields : Lnodca (ir) -> Lnodca ()
  ; this pass complains if a sealed field can be modified by an exported circuit or any
  ; circuit that is reachable from an exported circuit.
```
In the compiler, whole-program analysis checks whether an exported circuit of the compiled top-level contract modifies a sealed field. When a top-level contract imports a module and calls `initialize` inside its `constructor`, `initialize` is not exported as an entrypoint of the contract, and the compiler accepts the code cleanly.

Within the module file itself, constructors do not exist. Therefore, flagging sealed field assignments inside a `module` definition is a false positive.

## Solution

1. **Updated `CompactSealedFieldMutationInspection.java`**:
   - Added a check:
     ```java
     if (PsiTreeUtil.getParentOfType(assignExpr, CompactModuleDefinition.class) != null) {
       return;
     }
     ```
   - Used modern Java 25 pattern matching for reference extraction and sealed ledger resolution:
     ```java
     if (firstChild instanceof CompactReferenceExprImpl ref) {
       targetRef = ref;
     }
     ...
     if (resolved instanceof CompactLedgerDeclaration ledger && ledger.isSealed()) {
     ...
     ```
2. **Added Automated Regression Test**:
   - Added `testSealedFieldMutationInsideModuleAllowed()` to `CompactInspectionTest.java` verifying that initializing a sealed ledger within a module circuit produces zero warnings, while mutations in top-level contract circuits remain strictly flagged.

## Verification

1. **Automated Reproduction and Regression Testing**:
   - Prior to fix: `testSealedFieldMutationInsideModuleAllowed()` failed with `AssertionFailedError: Mutating sealed field inside module should produce no sealed field warnings: [HighlightInfo(..., description='Cannot modify sealed ledger field '_instanceSalt' outside constructor', ...)]`.
   - After fix: `CompactInspectionTest` passed 100%.
   - Full Gradle test suite (`./gradlew test`) ran cleanly with 0 failures and 0 warnings.
2. **MCP Code Inspection**:
   - Ran `lint_files` and `get_file_problems` on modified source and test files with zero errors and zero warnings.

## Prevention / Lesson

When creating AST-level syntax and semantic checks (like constructor restrictions), always account for language construct scoping rules. In languages where libraries or modules cannot have constructors, inspection rules that require a constructor parent must explicitly exempt module scopes to avoid false positives on standard library initialization patterns.

## Related Files

- `src/main/java/dev/verloren/midnight/inspection/CompactSealedFieldMutationInspection.java`
- `src/test/java/dev/verloren/midnight/inspection/CompactInspectionTest.java`
- `references/check-sealed-fields.ss`
- `compact-contracts/contracts/src/access/ShieldedAccessControl.compact`

## Related ADRs / Context

- None
