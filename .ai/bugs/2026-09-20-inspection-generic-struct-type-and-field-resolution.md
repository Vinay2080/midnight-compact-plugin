# False-Positive Unresolved Struct Field and Type Mismatch Errors on Generic Structs and Circuits

- **Date:** 2026-09-20
- **Feature / Component:** inspection / type system / PSI
- **Severity:** High
- **Status:** Resolved

## Symptoms

In `.compact` files, valid Compact code utilizing generic structs (e.g. `Either<T1, T2>`, `Either<Bytes<32>, ContractAddress>`) and generic circuits/witnesses reported multiple false-positive semantic errors:
1. `Unresolved struct field 'right'`, `'left'`, and `'is_left'` on member access expressions (e.g., `keyOrAddress.right`, `target.is_left`).
2. `Type mismatch: expected 'Either<T1, T2>', got 'Either'` on struct literal expressions (e.g., `Either<T1, T2> { ... }`).
3. `Type mismatch: expected 'Either<Bytes<32>, ContractAddress>', got 'Either'` on struct literal expressions.
4. `Type mismatch: expected 'Either<ZswapCoinPublicKey, ContractAddress>', got 'Either<A, B>'` on generic circuit calls (e.g., `right<ZswapCoinPublicKey, ContractAddress>(kernel.self())`).

## Context

The Compact Standard Library and contracts (e.g., OpenZeppelin `Utils.compact`) define and manipulate parameterized structs such as:
```compact
struct Either<A, B> {
  is_left: Boolean;
  left: A;
  right: B;
}
```
Circuits frequently pass and return parameterized instances of `Either`, accessing fields like `.is_left`, `.left`, and `.right` or returning generic call outputs and struct literals.

## Root Cause

1. **Struct Field Lookup Included Generic Type Arguments in Type Name**:
   In `CompactStructFieldReference.java`, field lookup extracted `baseType.name()` (e.g. `"Either<ZswapCoinPublicKey, ContractAddress>"`). Passing this parameterized string directly to `CompactResolveUtil.resolveType` failed because the struct declaration name in PSI is `"Either"`.
2. **Struct Literal Type Omitted Type Arguments**:
   In `CompactStructLiteralExprImpl.java`, `getType()` resolved the reference to the struct declaration and returned its raw name (`"Either"`), dropping explicit `GENERIC_ARGUMENT_LIST` tokens present on the expression (`<T1, T2>`).
3. **Generic Call Return Types Did Not Substitute Generic Arguments**:
   In `CompactCallExprImpl.java`, calling a generic callable like `right<ZswapCoinPublicKey, ContractAddress>(...)` returned the uninstantiated callee return type `Either<A, B>` without substituting call-site generic arguments.
4. **Member Expression Type Inference Ignored Named Element Types and Parameter Substitution**:
   In `CompactMemberExprImpl.java`, `getType()` only handled `CompactTypeElement` (which does not match `CompactStructFieldImpl`) and did not substitute the struct generic parameters (`[A, B]`) with the base expression type arguments (`[ZswapCoinPublicKey, ContractAddress]`).

## Solution

1. **Raw Type Name Extraction for Field Resolution**:
   Updated `CompactStructFieldReference.java` and `CompactTypeInferenceUtil.getRawTypeName` to strip generic type arguments prior to resolving struct declarations and type aliases.
2. **Parameterized Struct Literal Type Retention**:
   Updated `CompactStructLiteralExprImpl.java` to construct full parameterized types (e.g., `Either<T1, T2>`) when `GENERIC_ARGUMENT_LIST` is present on the literal.
3. **Generic Call Site Type Parameter Substitution**:
   Added generic argument parsing and parameter mapping in `CompactTypeInferenceUtil` and updated `CompactCallExprImpl.java` to instantiate callable return types with concrete call-site type arguments.
4. **Member Expression Field Type Inference & Parameter Mapping**:
   Updated `CompactMemberExprImpl.java` to resolve `CompactStructFieldImpl` types and substitute struct type parameters with the base expression's generic arguments.
5. **Generic Assignability Unification**:
   Enhanced `CompactPrimitiveType.isAssignableTo` and `CompactTypeInferenceUtil.isGenericAssignable` to handle assignability between instantiated and uninstantiated generic types containing type variables.

## Verification

- Added `CompactInspectionTest.testUtilsModuleEitherAndFieldResolutionNoWarnings` reproducing the exact `Utils.compact` code structure and validating zero unresolved field warnings or type mismatch errors.
- Executed the full automated verification harness (`.\scripts\verify-patch.ps1`), satisfying all gates and regression test suites.

## Prevention / Lesson

When building semantic analysis and type inference for generic languages, ensure:
1. Symbol resolution separates base/raw type identifiers from parameterized argument lists.
2. Expression AST types retain explicit generic argument lists.
3. Callable and member access type lookups systematically substitute type parameters with receiver/call-site arguments.

## Related Files

- `src/main/java/dev/verloren/midnight/reference/CompactStructFieldReference.java`
- `src/main/java/dev/verloren/midnight/type/CompactTypeInferenceUtil.java`
- `src/main/java/dev/verloren/midnight/type/CompactPrimitiveType.java`
- `src/main/java/dev/verloren/midnight/psi/CompactStructLiteralExprImpl.java`
- `src/main/java/dev/verloren/midnight/psi/CompactCallExprImpl.java`
- `src/main/java/dev/verloren/midnight/psi/CompactMemberExprImpl.java`
- `src/test/java/dev/verloren/midnight/inspection/CompactInspectionTest.java`
