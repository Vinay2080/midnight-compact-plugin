# False-Positive Type Mismatch Error on Ternary Return Statements

- **Date:** 2026-09-18
- **Feature / Component:** inspection / type system / PSI
- **Severity:** High
- **Status:** Resolved

## Symptoms

In `.compact` files, valid return statements returning a ternary conditional expression (e.g. `return cond ? a : b;`) reported a false-positive type mismatch error in the IntelliJ editor and inspection suite. Specifically, in `module.compact:89`:

```compact
export pure circuit canonicalize<T1, T2>(value: Either<T1, T2>): Either<T1, T2> {
  return value.is_left
      ? Either<T1, T2> { is_left: true, left: value.left, right: default<T2> }
      : Either<T1, T2> { is_left: false, left: default<T1>, right: value.right };
}
```

The editor inspection flagged `return value.is_left` on line 89 with:
```
Type mismatch: expected 'Either', got 'Boolean'
```

## Context

Compact supports ternary conditional expressions (`condition ? thenBranch : elseBranch`). The grammar and parser (`CompactParser.java:parseTernaryExpression`) produce an AST element of type `CompactElementTypes.TERNARY_EXPR`.

Within `CompactTypeMismatchInspection.java`, return statements are validated by querying:
```java
CompactExpression returnExpr = PsiTreeUtil.findChildOfType(returnStatement, CompactExpression.class);
```
and comparing `returnExpr.getType()` to `expectedType`.

## Root Cause

1. **Missing Dedicated PSI Class for `TERNARY_EXPR`**:
   `CompactElementTypes.TERNARY_EXPR` was not mapped in `CompactElementFactory.java` (`createElement` or `hasDedicatedElement`). As a result, IntelliJ constructed a fallback `CompactPsiElement` instance for the ternary node. Because `CompactPsiElement` does not implement `CompactExpression`, the ternary node itself was invisible to queries searching for `CompactExpression`.

2. **Skipping Ternary and Selecting the Condition Sub-expression**:
   When `CompactTypeMismatchInspection.checkReturnStatement` executed:
   ```java
   PsiTreeUtil.findChildOfType(returnStatement, CompactExpression.class)
   ```
   `PsiTreeUtil` performed a depth-first search for the first descendant implementing `CompactExpression`. Because the root ternary expression was only a generic `CompactPsiElement`, `findChildOfType` descended into its children and selected its condition (`value.is_left`, which is a `CompactMemberExprImpl` implementing `CompactExpression`).
   Since `value.is_left` has type `Boolean`, while the circuit `canonicalize` has return type `Either<T1, T2>`, the inspection reported:
   `Type mismatch: expected 'Either', got 'Boolean'`.

3. **No Type Inference for Ternary Expressions**:
   Because `TERNARY_EXPR` had no dedicated PSI class, it lacked type inference (`getType()`). Even if referenced directly, its evaluated type would default to `CompactPrimitiveType.UNKNOWN`.

4. **Missing Validation of Ternary Conditions and Branches**:
   `CompactTypeMismatchInspection` did not inspect ternary expressions to ensure the condition evaluates to `Boolean` and that both branches evaluate to mutually assignable types as mandated by the Compact Language Specification.

## Solution

1. **Created Dedicated PSI Interface and Implementation**:
   - Created `CompactTernaryExpr.java` extending `CompactExpression` with `getCondition()`, `getThenBranch()`, and `getElseBranch()`.
   - Created `CompactTernaryExprImpl.java` implementing `CompactTernaryExpr`.
     - `getCondition()` locates the first expression child prior to the `?` token.
     - `getThenBranch()` locates the expression child in the `EXPRESSION_SEQUENCE` between `?` and `:`.
     - `getElseBranch()` locates the expression child following the `:` token.
     - `getType()` infers the type from the `thenBranch` and `elseBranch` types, ensuring type propagation upward to enclosing return statements and assignments.

2. **Registered in `CompactElementFactory`**:
   - Added `CompactElementTypes.TERNARY_EXPR` to `createElement()` returning a `new CompactTernaryExprImpl(node)`.
   - Added `CompactElementTypes.TERNARY_EXPR` to `hasDedicatedElement()`.

3. **Extended `CompactTypeMismatchInspection`**:
   - Added `checkTernaryExpr(CompactTernaryExpr, ProblemsHolder)`:
     - Reports `"Boolean expected in ternary condition, got '<type>'"` if condition type is known and not `Boolean`.
     - Reports `"Type mismatch in ternary branches: '<thenType>' and '<elseType>'"` if branch types are known and not mutually assignable.
   - Dispatched `checkTernaryExpr` in `CompactTypeMismatchInspection.visitElement`.

## Verification

- **Automated Tests Added**:
  - `testReturnTypeMatchSimpleTernaryExpression`: Verifies `return cond ? 1 : 2;` in `Field` circuit produces 0 warnings.
  - `testReturnTypeMatchTernaryExpressionGenericStruct`: Verifies `return value.is_left ? Either { ... } : Either { ... };` in generic `Either` circuit produces 0 warnings.
  - `testReturnTypeMismatchTernaryExpression`: Verifies `return cond ? true : false;` in `Field` circuit reports `Type mismatch: expected 'Field', got 'Boolean'`.
  - `testTernaryConditionNonBooleanFails`: Verifies non-boolean condition reports `Boolean expected in ternary condition, got 'Field'`.
  - `testTernaryBranchesTypeMismatchFails`: Verifies incompatible branches report `Type mismatch in ternary branches: 'Boolean' and 'Field'`.

- **Continuous Inspection Verification**:
  - `lint_files` via IntelliJ MCP on all modified and new files: 0 errors, 0 warnings, 0 weak warnings.
  - `reformat_file` and encoding checks: 0 issues.

- **Full Test Suite**:
  - Executed `./gradlew test` with 100% pass rate across the full plugin test suite.

## Prevention / Lesson

- Every non-terminal element type declared in `CompactElementTypes` that represents an expression must have a dedicated PSI class implementing `CompactExpression` and registered in `CompactElementFactory`.
- When querying `PsiTreeUtil.findChildOfType(parent, CompactExpression.class)`, if a compound expression node lacks `CompactExpression`, the search silently bypasses the compound node and selects an inner sub-expression, causing baffling false-positive type mismatches.
