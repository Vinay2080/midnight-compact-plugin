# ADR-003: Lightweight Structural Type Inference Engine

## Status
Accepted (Phase 7 / Complete)

## Context & Problem Statement
IDE features like type mismatch inspections (`CompactTypeMismatchInspection`), parameter hints, hover documentation, and quick-fixes (`SpecifyTypeExplicitlyIntention`) require knowing the semantic types of expressions (e.g. `const x = 5 + y;`).
However, full Hindley-Milner type inference with bidirectional constraint solving is heavy, prone to performance bottlenecks during active typing, and unnecessary for Compact's predominantly explicitly-typed smart contract grammar.

## Authoritative References
1. **Official Midnight Compiler IR & Type Checking**:
   - [`compact/compiler/langs.ss:290-330`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/compact/compiler/langs.ss#L290-L330): Nanopass IR type representations for `Lsrc` (`tboolean`, `tfield`, `tunsigned`, `tbytes`, `topaque`, `tvector`, `ttuple`).
   - [`compact/compiler/typecheck.ss`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/compact/compiler/typecheck.ss): Upstream compiler type checker rules for operator typing, subtype checking, and numeric literal coercion.
2. **Production JetBrains Implementations**:
   - `intellij-rust`: [`RsTypeInferenceWalker.kt`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/intellij-rust/src/main/kotlin/org/rust/lang/core/types/infer/TypeInferenceWalker.kt) — fast AST-local structural type inference for expressions without full cross-crate whole-program analysis.

## Decision
1. **Lightweight Structural Type Hierarchy**:
   Implement a clean, immutable object model representing Compact types:
   - `CompactPrimitiveType`: `BOOLEAN`, `FIELD`, `UNKNOWN`, `VOID`.
   - `CompactNumericLiteralType`: Untyped numeric integer literals capable of coercion into `Field` or `Uint<N>`.
   - `CompactParameterizedType`: `Bytes<N>`, `Uint<N>`, `Vector<N, T>`.
   - `CompactTupleType`: `[T1, T2, ...]`.
   - `CompactNamedType`: Struct and Enum user-defined types.
2. **Local Expression Type Evaluator (`CompactTypeInferenceUtil`)**:
   - Evaluates boolean operations (`&&`, `||`, `==`, `!=`, `<`, `<=`, `>`, `>=`) -> `Boolean`.
   - Evaluates arithmetic operations (`+`, `-`, `*`, `/`, `%`) -> propagates operand types with numeric literal coercion.
   - Evaluates unary operations (`!`, `-`).
   - Resolves reference expressions to their declared binding type (`const x: Field` -> `Field`).
3. **Graceful Fallback on Incomplete Code**:
   Returns `CompactPrimitiveType.UNKNOWN` whenever AST expressions are incomplete or types cannot be resolved, avoiding false-positive inspections.

## Scalability & Anti-Hardcoding Evaluation
- **Is it hardcoded?**: No. It maps operator token families to their mathematical result types based on the upstream language specification.
- **Is it scalable?**: Yes. New operators or composite types can be added to the inference evaluator without modifying AST node implementations.

## Feature Implementation Map
- Inference Engine: [`CompactTypeInferenceUtil.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/type/CompactTypeInferenceUtil.java)
- Type Model Hierarchy:
  - [`CompactType.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/type/CompactType.java)
  - [`CompactPrimitiveType.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/type/CompactPrimitiveType.java)
  - [`CompactNumericLiteralType.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/type/CompactNumericLiteralType.java)
- Expression Typing: `CompactExpression.getType()` on PSI element implementations.
- Unit Tests: [`CompactTypeInferenceTest.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/type/CompactTypeInferenceTest.java)

## Consequences & Future Maintenance
- Expression types are computed on-demand and cached per PSI element lifecycle via IntelliJ's internal AST caching.
- When expanding to generic contracts or complex type aliases in future phases, the type hierarchy is already decoupled from PSI syntax nodes.
