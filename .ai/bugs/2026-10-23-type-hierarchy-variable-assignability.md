# Type Variable Over-Generalization and Assignability Collapse in Type Inference

- **Date:** 2026-10-23
- **Feature / Component:** type system / inference / inspection / completion
- **Severity:** High
- **Status:** Resolved

## Symptoms

Running the full automated test suite failed with 9 failing tests across 4 separate test suites:
1. `CompactTypeHierarchyTest`:
   - `testParseTypePrimitivesAndNumericLiterals`: `AssertionFailedError: expected:<Boolean> but was:<Boolean>`
   - `testTypeSubstitutorDirectVariableReplacement`: `AssertionFailedError: expected:<Z> but was:<null>`
2. `CompactCompletionTest`:
   - `testReturnCompletionStructTypeAwareness`: `Matching Token type 't' (1) should precede Field type 'n' (0)`
   - `testReturnCompletionBooleanTypeAwareness`: `Matching Boolean type 'flag' (1) should precede Uint type 'amount' (0)`
3. `CompactReturnTypeInspectionTest`:
   - `testReturnTypeMismatchTernaryExpression`: expected 1 mismatch warning but got 0
   - `testTernaryBranchesTypeMismatchFails`: expected 1 branch mismatch warning but got 0
   - `testReturnTypeMismatchBooleanIntoField`: expected 1 mismatch warning for `return true` in `Field` circuit but got 0
4. `CompactTypeMismatchInspectionTest`:
   - `testUintIncompatibleComparisonsNegative`: expected 5 cannot compare warnings but got 3
   - `testEqualityTypeMismatch`: expected 1 cannot compare warning for `true == 1` but got 0

## Context

During recent type hierarchy and substitutor additions (`CompactTypeSubstitutor` and `CompactTypeVariable`), multi-letter generic parameter support (such as `TVal` and `TErr` on user-defined structs like `Result<TVal, TErr>`) was introduced into `CompactTypeInferenceUtil`.

## Root Cause

1. **Over-Broad Type Variable Regex in `isTypeVariable`**:
   `CompactTypeInferenceUtil.isTypeVariable` contained `cleanName.matches("^[A-Z][a-zA-Z0-9]*$")`. This regex matched every PascalCase identifier, causing all builtin primitive types (`Boolean`, `Field`, `Bytes`, `Opaque`) as well as all user-defined struct types (`Token`, `Result`, etc.) to be identified as type variables (`CompactTypeVariable`).
2. **Missing Primitive Priority in `parseType`**:
   `CompactTypeInferenceUtil.parseType` checked `isTypeVariable(trimmed)` before falling back to `CompactPrimitiveType`. Because `isTypeVariable` returned true for `"Boolean"`, `parseType("Boolean")` returned `CompactTypeVariable("Boolean")` rather than `CompactPrimitiveType.BOOLEAN`.
3. **Unbound Type Variable Universal Assignability**:
   `CompactTypeVariable.isAssignableTo` unifies with any type. Because primitive and struct types were parsed as type variables, `Boolean` unified with `Field`, `Uint` unified with `Boolean`, and `Token` unified with `Field`. This suppressed type mismatch warnings and degraded type-aware completion ranking.
4. **Substitutor Variable Overload Returning Null for Unmapped Variables**:
   In `CompactTypeSubstitutor`, `public @Nullable CompactType substitute(@NotNull CompactTypeVariable variable)` performed a direct map lookup returning `null` when a variable was unmapped. When callers invoked `substitute(new CompactTypeVariable("Z"))`, Java static overload resolution chose the specific `CompactTypeVariable` overload rather than the `CompactType` overload, returning `null` instead of preserving the unmapped variable.
5. **Loss of Base Type Checking in `isGenericAssignable`**:
   `CompactTypeInferenceUtil.isGenericAssignable` had been altered to invoke `parseType(from).isAssignableTo(parseType(to))` without verifying that the raw base type names matched (`getRawTypeName(from).equals(getRawTypeName(to))`), bypassing nominal type separation.

## Investigation

Tracing test failure logs from `verify-patch.ps1 -AllTests` revealed:
- `testParseTypePrimitivesAndNumericLiterals` reported `expected:<Boolean> but was:<Boolean>`, revealing that both objects rendered the same string but were instances of different classes (`CompactPrimitiveType` vs `CompactTypeVariable`).
- Inspecting `isTypeVariable` highlighted the regex `^[A-Z][a-zA-Z0-9]*$`.
- Inspecting `CompactPrimitiveType.isAssignableTo` and `isGenericAssignable` revealed that `CompactPrimitiveType` called `isGenericAssignable(this.name, other.name())`, which lacked raw base type guards.

## Solution

1. **Primitive Factory & Guard in `CompactPrimitiveType`**:
   - Added `CompactPrimitiveType.fromName(@NotNull String name)` returning the singleton instances for `Boolean`, `Field`, `Bytes`, `Opaque`, and `Unknown`.
   - Updated `CompactPrimitiveType.isAssignableTo` to only evaluate `isGenericAssignable` when both types share matching raw base names and parameter brackets.
2. **Constrained `isTypeVariable` & Prioritized `parseType`**:
   - In `CompactTypeInferenceUtil.isTypeVariable`, added early exclusion for `CompactPrimitiveType.fromName(name) != null`.
   - Constrained type variable matching to single uppercase letters with optional digits (`^[A-Z][0-9]?$`), numbered type parameters (`^T[0-9]+$`), `T` followed by uppercase letters (`^T[A-Z][a-zA-Z0-9]*$`, e.g. `TVal`, `TErr`), and `#`-prefixed variables.
   - In `CompactTypeInferenceUtil.parseType`, prioritized `CompactPrimitiveType.fromName(trimmed)` at the top before type variable detection.
   - Restored base type equality and argument assignability checks in `CompactTypeInferenceUtil.isGenericAssignable`.
3. **Substitutor Unmapped Variable Invariant**:
   - In `CompactTypeSubstitutor`, updated `substitute(@NotNull CompactTypeVariable variable)` to return `@NotNull CompactType`, returning `variable` unchanged when not found in substitutions.
   - Simplified the pattern-matching switch in `substitute(@NotNull CompactType type)`.
4. **Mirror Tests**:
   - Added `testPrimitiveAndStructTypeVariableDistinctionMirror` in `CompactTypeHierarchyTest` verifying that primitives and user structs are never identified as type variables and maintain strict mutual non-assignability.

## Verification

1. Targeted test verification passed:
   - `CompactTypeHierarchyTest`
   - `CompactCompletionTest`
   - `CompactReturnTypeInspectionTest`
   - `CompactTypeMismatchInspectionTest`
2. Full multi-gate verification runner passed (`.\scripts\verify-patch.ps1 -AllTests -Clean`):
   - Gate 0 (Git Boundary): Passed
   - Gate 1 (Compilation Java 25): Passed
   - Gate 1b (Static Analysis & Checkstyle): Passed
   - Gate 2 (Plugin Structure & Verification): Passed
   - Gate 3 (Unit Tests): All 765 tests passed with 0 failures

## Prevention / Lesson

1. Never use unrestricted regexes (`^[A-Z][a-zA-Z0-9]*$`) for type variables; generic placeholder naming heuristics must never encompass nominal types or builtin primitives.
2. Type inference engines must strictly separate builtin primitives, nominal struct/enum types, and type variables.
3. Method overloads accepting subtype interfaces in visitors/substitutors must have consistent default behaviors (identity substitution for unmapped nodes) to avoid static overload surprises.

## Related Files

- `src/main/java/dev/verloren/midnight/type/CompactPrimitiveType.java`
- `src/main/java/dev/verloren/midnight/type/CompactTypeInferenceUtil.java`
- `src/main/java/dev/verloren/midnight/type/CompactTypeSubstitutor.java`
- `src/test/java/dev/verloren/midnight/type/CompactTypeHierarchyTest.java`

## Related ADRs / Context

- `.ai/bugs/2026-09-20-inspection-generic-struct-type-and-field-resolution.md`
- `.agents/rules/architecture.rules.md`
