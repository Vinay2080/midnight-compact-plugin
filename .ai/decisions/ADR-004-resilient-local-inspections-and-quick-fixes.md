# ADR-004: Resilient Semantic Inspections Suite & Quick-Fixes

## Status
Accepted (Phases 8–14, 25 / Complete)

## Context & Problem Statement
Smart contracts require rigorous static verification before deployment to prevent blockchain reverts, gas depletion, or zero-knowledge proof generation failures.
Specifically, Compact contracts enforce strict domain invariants:
1. **Circuit Purity**: A `pure circuit` cannot mutate or query ledger state.
2. **Sealed Ledger Immutability**: `sealed` ledger state fields can only be initialized inside constructors and never mutated in circuits.
3. **Witness Disclosure**: Private witness values cannot leak into public outputs or ledger state without an explicit `disclose(...)`.
4. **Recursion Prevention**: Circuits cannot be recursive (ZKP constraint generation requires unrolled static call graphs).
5. **Constructor Constraints**: Constructors cannot return values and cannot declare witnesses.
6. **Pragma Version Compatibility**: Source files must match the active compiler toolchain.

These checks must execute in real time in the editor without blocking the UI thread or throwing exceptions on incomplete syntax.

## Authoritative References
1. **Official Midnight Compiler Semantics**:
   - [`compact/compiler/langs.ss`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/compact/compiler/langs.ss) & [`compact/compiler/typecheck.ss`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/compact/compiler/typecheck.ss): Compiler errors for impure circuits, undisclosed witnesses, recursive circuits, and pragma violations.
2. **Production JetBrains Implementations**:
   - `intellij-rust`: [`RsLocalInspectionTool.kt`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/intellij-rust/src/main/kotlin/org/rust/ide/inspections/RsLocalInspectionTool.kt) and quick-fix implementations.
   - `intellij-scala`: [`ScalaLocalInspection.scala`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/intellij-scala/scala/scala-impl/src/org/jetbrains/plugins/scala/codeInspection/ScalaLocalInspection.scala).

## Decision
1. **Local Inspection Architecture (`LocalInspectionTool`)**:
   - Implement all semantic checks as distinct `LocalInspectionTool` subclasses registered in `plugin.xml`.
   - Each inspection uses a `CompactVisitor` to traverse only relevant AST nodes, yielding immediately if cancelled by `ProgressManager.checkCanceled()`.
2. **Tolerance for Incomplete Code**:
   Every inspection guards against `PsiErrorElement`, unclosed nodes, and `null` types, ensuring that incomplete typing does not produce bogus warnings or exceptions.
3. **One-Click Quick-Fixes (`LocalQuickFix`)**:
   - `CompactRemoveUnusedVariableFix`: Deletes unused variable bindings cleanly.
   - `CompactSwitchCompilerVersionIntention`: Switches project toolchain to match pragma.
   - `CompactUpdatePragmaVersionIntention`: Updates file pragma to match active compiler.
   - `CompactSpecifyTypeExplicitlyIntention` / `CompactRemoveRedundantTypeIntention`.

## Scalability & Anti-Hardcoding Evaluation
- **Is it hardcoded?**: No. Inspections evaluate semantic properties of the PSI tree (e.g., examining the call graph for cycles to detect recursion, checking modifiers on target elements, comparing normalized SemVer tuples) rather than string matching.
- **Are quick-fixes atomic?**: Yes. All modifications run inside `WriteCommandAction` on the EDT with complete undo/redo support.

## Feature Implementation Map
- Inspections Suite:
  - [`CompactUnresolvedReferenceInspection.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/inspection/CompactUnresolvedReferenceInspection.java)
  - [`CompactDuplicateDeclarationInspection.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/inspection/CompactDuplicateDeclarationInspection.java)
  - [`CompactUnusedLocalVariableInspection.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/inspection/CompactUnusedLocalVariableInspection.java)
  - [`CompactTypeMismatchInspection.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/inspection/CompactTypeMismatchInspection.java)
  - [`CompactPureCircuitInspection.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/inspection/CompactPureCircuitInspection.java)
  - [`CompactSealedFieldMutationInspection.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/inspection/CompactSealedFieldMutationInspection.java)
  - [`CompactRecursiveCircuitInspection.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/inspection/CompactRecursiveCircuitInspection.java)
  - [`CompactConstructorRestrictionInspection.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/inspection/CompactConstructorRestrictionInspection.java)
  - [`CompactUndisclosedWitnessInspection.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/inspection/CompactUndisclosedWitnessInspection.java)
  - [`CompactPragmaVersionInspection.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/inspection/CompactPragmaVersionInspection.java)
- Registration: [`plugin.xml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/resources/META-INF/plugin.xml) (`<localInspection>`)
- Test Suites: 10 dedicated inspection test classes under `src/test/java/dev/verloren/midnight/inspection/`.

## Consequences & Future Maintenance
- When adding new compiler constraints, implement them as standalone `LocalInspectionTool` classes with companion unit tests before creating quick-fixes.
