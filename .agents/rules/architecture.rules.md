# Path Rules: Architecture, Modularity, Generalization & Code Quality Standards

**Scope:** `src/main/java/dev/verloren/midnight/**`, `src/test/java/dev/verloren/midnight/**`

## Core Architectural Invariants

1. **Layer Hierarchy & Dependency Flow**:
   - Dependencies must flow **downward only**:
     `[UI / Completion / Inspection / Editor]` -> `[Resolution & Scopes]` -> `[Type Engine]` -> `[PSI / Parser / Lexer]`.
   - **STRICTLY PROHIBITED**: Upward dependencies where core semantic packages (`type`, `resolve`, `psi`) import UI or completion classes (`completion`, `editor`, `inspection`).
   - **STRICTLY PROHIBITED**: Implementing inline type unification or scope resolution logic inside completion providers or editor handlers. Always delegate to `CompactType` and `CompactResolveUtil`.

2. **The Rule of Generalization (Anti-Hardcoding Invariant)**:
   - Any language capability (e.g. struct literal completion, constructor helper inference, generic argument matching, member access) must apply universally to **all** struct definitions and circuits.
   - **STRICTLY PROHIBITED**: Hardcoding standard library names (`"Either"`, `"Maybe"`, `"Vector"`, `"default"`) in general-purpose compiler, completion, or inspection code to achieve special behavior.
   - **Mandatory "User-Defined Mirror" Testing**: Every test exercising standard library constructs (`Either`, `Maybe`) must have an identical companion test exercising user-defined generic structs (e.g. `Result<TVal, TErr>`, `Pair<A, B>`).

3. **Type System as Single Source of Truth**:
   - All type representations must be instances of the sealed `CompactType` hierarchy (`CompactPrimitiveType`, `CompactParameterizedType`, `CompactStructType`, `CompactTypeVariable`, `CompactFunctionType`).
   - **STRICTLY PROHIBITED**: Passing raw type strings (e.g. `"Either<Field, Boolean>"`) and parsing with regex or substring split operations.
   - Type parameter binding and substitution must be executed via `CompactTypeSubstitutor`.

4. **Modularity & Single Responsibility Principle (SRP)**:
   - **File Length Limit**: Every class must remain **<= 400 lines**. Classes exceeding this limit must be split into dedicated providers, visitors, or handlers.
   - **Method Length Limit**: Methods must remain **<= 40 lines**.
   - **Completion Modularity**: `CompletionContributor` only registers patterns; distinct `CompletionProvider` subclasses compute lookup items; distinct `InsertHandler` subclasses handle document mutations.
   - **Inspection Modularity**: Inspections only configure error reporting; semantic validation is delegated to shared `CompactTypeChecker` or `CompactResolveUtil`.

5. **Threading & Memory Safety**:
   - PSI reads must be wrapped in `ReadAction` or executed on background threads.
   - Document and PSI modifications must be wrapped in `WriteCommandAction` on the EDT.
   - **STRICTLY PROHIBITED**: Storing `PsiElement`, `PsiFile`, `Document`, or `Project` in `static` fields or non-disposable caches.
   - Cross-file queries must be cached using `CachedValuesManager` keyed on `PsiModificationTracker.MODIFICATION_COUNT`.

6. **Modern Java 25 Idioms**:
   - All data carriers must be Java `record`s.
   - Pattern matching with switch expressions (`switch (type) { case CompactParameterizedType p -> ...; }`) must be used for algebraic data dispatch.
   - Sequenced collections (`getFirst()`, `getLast()`) and multi-line text blocks (`"""..."""`) are mandatory.
