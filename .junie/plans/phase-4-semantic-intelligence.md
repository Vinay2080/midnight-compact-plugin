# Phase 4 — Semantic Intelligence Implementation Plan

## Overview
Phase 4 provides semantic intelligence on top of the handwritten parser (Phase 2) and reference resolution (Phase 3). It introduces a lightweight type system, semantic highlighting, and code inspections.

## 1. Verified Architecture (Current State)
- **Parser/PSI (Phase 2):** Handwritten `CompactParser` and `CompactElementFactory` producing ASTWrapper-based PSI.
- **References (Phase 3):** `CompactResolveUtil` handles scope walking. `CompactReferenceBase` supports Value, Type, Enum Member, and Import resolution.
- **Type Inference (Phase 4B Preliminary):** 
    - `CompactType` and `CompactPrimitiveType` interfaces.
    - `CompactExpression` and `CompactTypeElement` PSI interfaces.
    - `getType()` implemented for Literals, References, Calls, Struct Literals, and Member Access.
    - `CompactStructFieldReference` uses inferred base type for field resolution.

## 2. Phase 4A: Semantic Model + Symbols/Scopes
The semantic model maps PSI elements to symbols that represent the "meaning" of declarations.

### Symbols
- **Interface:** `CompactSymbol`
- **Implementations:**
    - `CompactValueSymbol`: Circuits, Witnesses, Constants, Parameters, Ledger bindings.
    - `CompactTypeSymbol`: Structs, Enums, Type Aliases, Builtin Types.
    - `CompactModuleSymbol`: Modules.
- **Responsibility:** Hold visibility, origin PSI, and type information.

### Scopes
- Use existing `CompactResolveUtil` logic but wrap results in `CompactSymbol`.
- Modules and Contracts provide "export scopes" for member lookup.

## 3. Phase 4B: Type System + Type Inference (Staged)
Built upon the implemented `getType()` infrastructure.

### Rules
- **Builtin Types:** Boolean, Field, Address, Uint<n>, Bytes<n>, Vector<n, T>.
- **User Types:** Nominal typing for Structs and Enums.
- **Inference:**
    - Binop: Result type based on operand types (e.g., `Field + Field -> Field`).
    - Ternary: Common supertype of branches.
    - Call: Return type of resolved circuit/witness.
- **Generics:** Substitution of `T` in `Vector<n, T>` during member access/calls.

## 4. Phase 4C: Semantic Highlighting
Implemented via `Annotator` to distinguish between declaration types.

### Implementation
- **Class:** `CompactSemanticHighlightingAnnotator`
- **Mappings:**
    - Circuits/Witnesses -> `COMPACT_FUNCTION`
    - Structs/Enums -> `COMPACT_TYPE_NAME`
    - Parameters -> `COMPACT_PARAMETER`
    - Ledger Bindings -> `COMPACT_LEDGER_BINDING`

## 5. Phase 4D: Inspections + Quick Fixes
AST-walking inspections using `CompactVisitor`.

### Initial Inspections
- **Unresolved Reference:** Error if `ref.resolve() == null`. Quick fix: Create declaration (if safe).
- **Type Mismatch:** Error if `expr.getType()` not assignable to expected type.
- **Duplicate Declaration:** Check `CompactResolveUtil.resolveLocal` for existing same-name symbols.
- **Invalid Member Access:** Check if field exists in struct/enum.

## 6. Implementation Plan (File-by-File)

### Phase 4A: Semantic Model
- `src/main/java/dev/verloren/midnight/symbol/CompactSymbol.java` (New)
- `src/main/java/dev/verloren/midnight/symbol/CompactValueSymbol.java` (New)
- `src/main/java/dev/verloren/midnight/symbol/CompactTypeSymbol.java` (New)
- `src/main/java/dev/verloren/midnight/resolve/CompactResolveUtil.java` (Modify): Return Symbols instead of PSI where appropriate.

### Phase 4C: Highlighting
- `src/main/java/dev/verloren/midnight/highlighter/CompactSemanticHighlightingAnnotator.java` (New)
- `src/main/java/dev/verloren/midnight/highlighter/CompactHighlightingColors.java` (New)

### Phase 4D: Inspections
- `src/main/java/dev/verloren/midnight/inspection/CompactInspectionBase.java` (New)
- `src/main/java/dev/verloren/midnight/inspection/UnresolvedReferenceInspection.java` (New)
- `src/main/java/dev/verloren/midnight/inspection/TypeMismatchInspection.java` (New)

## 7. Performance Considerations
- **Caching:** Use `CachedValuesManager` for `getType()` results.
- **Scope Walking:** Limit recursion depth in `CompactResolveUtil`.
- **Incomplete Code:** `getType()` must always return `CompactPrimitiveType.UNKNOWN` instead of throwing or returning null.

## 8. Risks & Open Questions
- **Cross-File Resolution:** Currently restricted to single-file. Future indexing might be needed for `import` across files.
- **Complex Generics:** Full unification is avoided; only simple substitution is planned.
