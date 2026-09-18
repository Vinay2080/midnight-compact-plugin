# Parameter and In-Scope Variable Autocompletion Suppressed in Return Statements

- **Date:** 2026-09-18
- **Feature / Component:** completion / type system / return expressions
- **Severity:** High
- **Status:** Resolved

## Symptoms

When writing circuits or functions in `.compact` files, in-scope parameters or variables whose static type differed from the return type were completely omitted from code autocompletion suggestions inside return statements:

```compact
export pure circuit isContractAddress(keyOrAddress: Either<ZswapCoinPublicKey, ContractAddress>): Boolean {
  return !keyOrAddress.is_left;
}
```

When typing in the return statement (e.g. `return !<caret>` or `return <caret>`), pressing completion (`Ctrl+Space`) did not offer `keyOrAddress`. This prevented users from typing the variable to access its members (e.g. `.is_left`) or pass it into sub-expressions.

## Context

In IntelliJ Platform language plugins, basic completion (`CompletionType.BASIC`) in value/expression contexts should present all accessible in-scope declarations (parameters, local constants, witnesses, circuits, imports). When an expected type is inferred from the enclosing callable (e.g., return type `Boolean`), type compatibility should be used for **weighting and prioritizing** suggestions to the top of the popup, rather than completely suppressing inaccessible or non-matching declarations. Suppressing non-matching types prevents legitimate use cases such as field access, unary operations, or method calls on those objects.

## Root Cause

In `CompactCompletionContributor.java:addValueCompletions`:

```java
if (expectedType != null && !CompactPrimitiveType.UNKNOWN.equals(expectedType)) {
  Collection<CompactNamedElement> allDecls = CompactResolveUtil.collectValueDeclarations(position);
  Set<String> seen = new HashSet<>();

  for (CompactNamedElement decl : allDecls) {
    CompactType declType = getCandidateType(decl);
    if (isTypeCompatible(declType, expectedType)) {
      String name = decl.getName();
      if (name != null && seen.add(name)) {
        addNamed(result, decl);
      }
    }
  }

  // Add compatible expression keywords with high priority
  if (isTypeCompatible(CompactPrimitiveType.BOOLEAN, expectedType)) {
    result.addElement(PrioritizedLookupElement.withPriority(
        LookupElementBuilder.create("true").bold(), 100.0));
    result.addElement(PrioritizedLookupElement.withPriority(
        LookupElementBuilder.create("false").bold(), 100.0));
  }
  if (!"Void".equalsIgnoreCase(expectedType.name())) {
    result.addElement(PrioritizedLookupElement.withPriority(
        LookupElementBuilder.create("default"), 50.0));
    result.addElement(PrioritizedLookupElement.withPriority(
        LookupElementBuilder.create("disclose"), 50.0));
  }
  return; // BUG: early return drops all non-matching in-scope declarations
}
```

When an expected type like `Boolean` was inferred:
1. Only declarations whose static type matched `Boolean` were added (at default priority `0.0`).
2. `true` and `false` were added with priority `100.0`.
3. An early `return;` terminated the method, permanently discarding all parameters (like `keyOrAddress: Either<...>`), local constants, witnesses, and imported values whose static type did not match `Boolean`.

## Investigation

Inspecting standard IntelliJ Platform completion guidelines and existing test cases revealed:
1. `testReturnCompletionFieldTypeAwareness`, `testReturnCompletionUintTypeAwareness`, `testReturnCompletionBooleanTypeAwareness`, and `testReturnCompletionStructTypeAwareness` originally hardcoded `assertFalse(lookupStrings.contains(...))` for parameters of differing types.
2. In Compact smart contracts, complex types like `Either<A, B>` or `struct` instances are frequently inspected within return statements to evaluate boolean conditions (e.g. `return !keyOrAddress.is_left;`, `return p.x == 0;`).
3. Dropping in-scope declarations in basic completion violates developer expectations, as variables are essential building blocks for expressions.

## Solution

1. **Prioritization Over Exclusion in `CompactCompletionContributor.java`**:
   - Added an overloaded `addNamed(@NotNull CompletionResultSet result, @NotNull CompactNamedElement element, double priority)`.
   - Updated `addValueCompletions` when `expectedType` is present:
     * In-scope declarations compatible with `expectedType` receive higher priority (`110.0`).
     * In-scope declarations of differing types receive standard priority (`20.0`), keeping them accessible for member access or sub-expressions.
     * Compatible literals (`true`, `false`) receive priority `100.0`.
     * Keywords `default` and `disclose` receive priority `50.0`.
     * Prefixed imports and general value keywords receive priority `10.0`.
     * Non-value statement keywords (e.g. `circuit`, `const`, `return`) remain excluded from value contexts.
2. **Fixed Pragma Space Insert Handler**:
   - Updated `createPragmaInsertHandler` in `CompactCompletionContributor.java` to check `chars.charAt(tailOffset) != ' '` rather than `!Character.isWhitespace(...)` so a trailing space is always inserted before trailing newlines.
3. **Automated Regression Tests**:
   - Added `testReturnValueCompletionSuggestsParameterInDirectReturn()` and `testReturnValueCompletionSuggestsParameterInReturnUnaryExpr()` to `CompactCompletionTest.java`.
   - Updated existing return type awareness tests (`testReturnCompletionFieldTypeAwareness`, `testReturnCompletionUintTypeAwareness`, `testReturnCompletionBooleanTypeAwareness`, `testReturnCompletionStructTypeAwareness`) to assert priority ordering (`indexOf(matching) < indexOf(nonMatching)`) instead of excluding in-scope parameters.

## Verification

1. **Automated Tests**:
   - Before fix: `testReturnValueCompletionSuggestsParameterInDirectReturn` failed with `AssertionFailedError: Should suggest parameter 'keyOrAddress' in return expression`.
   - After fix: All 80 completion tests in `CompactCompletionTest` passed.
   - Full test suite (`./gradlew test`) ran cleanly: 687 tests completed, 0 failures, 0 skipped.
2. **MCP Code Inspections**:
   - `get_file_problems` and `lint_files` reported 0 errors, 0 warnings, and 0 weak warnings on `CompactCompletionContributor.java` and `CompactCompletionTest.java`.

## Prevention / Lesson

Expected-type analysis in basic code completion (`CompletionType.BASIC`) should prioritize matching candidates over non-matching candidates, not eliminate valid in-scope variables. Developers routinely type in-scope variables in return statements to access fields, invoke methods, or compose boolean and arithmetic expressions.

## Related Files

- `src/main/java/dev/verloren/midnight/completion/CompactCompletionContributor.java`
- `src/test/java/dev/verloren/midnight/completion/CompactCompletionTest.java`
- `.ai/bugs/README.md`
