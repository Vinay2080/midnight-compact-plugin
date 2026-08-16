# ADR-003: Lightweight Structural Type Inference

## Context
Code editor features such as struct field navigation, dot-completion, and type-mismatch inspections require type information for expressions. However, full compiler-grade type inference involves constraint solving and global unification, which is too heavyweight for interactive typing.

## Decision
Implement a lightweight local type inference engine (`CompactTypeInferenceUtil` + `CompactType` hierarchy):
1. Support primitive types (`Boolean`, `Field`, `Uint`, `Bytes`, `Opaque`, `Void`, `Unknown`).
2. Support nominal user types (`CompactNamedType` for structs and enums).
3. Evaluate types locally for literals, binary operators, member accesses, and casts.
4. Fall back safely to `CompactPrimitiveType.UNKNOWN` whenever type information is incomplete or ambiguous.

## Alternatives Considered
1. **Full Constraint Solver**: Porting upstream Chez Scheme constraint unification into Java.
2. **No Type System**: Relying purely on name matching for struct fields.

## Why
- Provides editor-visible features (field navigation, type mismatch warnings) instantaneously with zero UI stutter.
- Robust against malformed code and intermediate typing states.
- Prevents cascading false-positive warnings by returning `UNKNOWN` when indeterminate.

## Consequences
- Complex generic constraint solving is deferred.
- Editor features remain fast, resilient, and accurate for all standard contracts.
