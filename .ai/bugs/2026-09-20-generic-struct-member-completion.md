# Member Autocompletion for Generic Struct Variables

- **Date:** 2026-09-20
- **Feature / Component:** completion / type system / PSI
- **Severity:** Medium
- **Status:** Resolved

## Symptoms

In `.compact` files, when attempting member autocompletion on variables with parameterized types (such as `target: Either<Bytes<32>, ContractAddress>` &rarr; `target.<caret>`), IntelliJ failed to suggest the struct fields (`is_left`, `left`, `right`).

## Context

Compact contracts and standard library structures frequently use generic structs (e.g. `Either<A, B>`). When developers type `target.` on an in-scope variable with type `Either<...>`, autocompletion should immediately present all declared fields on the struct.

## Root Cause

In `CompactCompletionContributor.java:addMembersFromTypeName(typeName, context, result)`, the method passed `typeName` (which evaluated to `"Either<Bytes<32>, ContractAddress>"`) directly to `CompactResolveUtil.resolveType`.
Because struct declarations in PSI are indexed by their bare identifier (`"Either"`), searching with the full parameterized string failed to find the struct definition or its fields.

## Solution

1. **Raw Type Name Extraction in Member Completion**:
   Updated `CompactCompletionContributor.java` to extract the raw type identifier via `CompactTypeInferenceUtil.getRawTypeName(typeName)` before querying `CompactResolveUtil.resolveType`.
2. **Type Alias Support**:
   Added unwrapping of `CompactTypeDefinitionImpl` aliases so that member completion resolves through type aliases pointing to struct definitions.

## Verification

- Added unit test `CompactCompletionTest.testGenericStructMemberCompletion` asserting that `target.<caret>` on `target: Either<Bytes<32>, ContractAddress>` suggests `is_left`, `left`, and `right`.
- Verified entire `CompactCompletionTest` suite passes cleanly via `verify-patch.ps1`.

## Prevention / Lesson

Any completion or resolution utility receiving a `CompactType` string must account for parameterized/generic arguments (`<...>`) before performing lexical declaration lookups against PSI declarations.

## Related Files

- `src/main/java/dev/verloren/midnight/completion/CompactCompletionContributor.java`
- `src/test/java/dev/verloren/midnight/completion/CompactCompletionTest.java`
