# ADR-022: Structural Editor Suite: Declarative Inlay Hints, Structure View, Breadcrumbs, & Folding

## Status
Accepted (Phases 14–16 / Complete)

## Context & Problem Statement
Reading complex zero-knowledge contracts with nested circuits, witnesses, and parameter lists can be overwhelming. Developers need structural readability aids:
1. **Declarative Inlay Hints**: Complex circuit calls with multiple numeric parameters (e.g. `mint(account, 100, 0, true)`) are prone to argument ordering mistakes without inline parameter labels (`account:`, `amount:`, `fee:`, `isPublic:`).
2. **Structure View (`Alt+7` / `Cmd+7`)**: Developers need a hierarchical tree outline of the contract's declarations (contracts, modules, circuits, witnesses, ledger fields, types) with quick filter and alphabetization support.
3. **Breadcrumbs**: When scrolling inside deeply nested blocks, developers need a horizontal path indicator showing the enclosing contract, circuit, and statement context.
4. **Code Folding**: Large contracts benefit from collapsing imports, block comments, contracts, circuits, and struct bodies.

## Authoritative References
1. **IntelliJ Editor Insight APIs**:
   - Modern Declarative Inlay Hints: `com.intellij.codeInsight.hints.declarative.InlayHintsProvider`
   - Structure View: `PsiStructureViewFactory`, `StructureViewModel`, `StructureViewTreeElement`
   - Breadcrumbs: `BreadcrumbsInfoProvider`
   - Code Folding: `CustomFoldingBuilder`
2. **Production JetBrains Implementations**:
   - `intellij-rust`: `RsInlayParameterHintsProvider.kt`, `RsStructureViewModel.kt`, `RsFoldingBuilder.kt`.

## Decision
1. **Modern Declarative Inlay Hints (`CompactInlayHintsProvider`)**:
   - Implements IntelliJ's Declarative Inlay Hints API (`InlayHintsProvider`, `SharedBypassCollector`), avoiding legacy scheduled-for-removal parameter hints APIs.
   - At call sites (`CompactCallExpr`), resolves the target circuit, witness, or constructor via `call.resolveCallee()`.
   - Compares formal parameters with argument expressions, emitting non-intrusive inline label hints (`<paramName>:`) before argument values.
   - Suppresses redundant hints (e.g. when the argument identifier matches the parameter name).
2. **Hierarchical Structure View (`CompactStructureViewModel`)**:
   - Maps AST hierarchy into a navigable tree: `CompactFile` -> `CompactContractDefinition` / `CompactModuleDefinition` -> `CompactCircuitDefinition` / `CompactWitnessDeclaration` / `CompactLedgerDeclaration`.
   - Assigns distinct official icons (`MidnightIcons`) to distinguish on-chain circuits, off-chain witnesses, and ledger state cells at a glance.
3. **Contextual Breadcrumbs (`CompactBreadcrumbsProvider`)**:
   - Computes concise breadcrumb strings for enclosing scopes (e.g. `contract Token` > `circuit mint`).
4. **Resilient Code Folding (`CompactFoldingBuilder`)**:
   - Creates folding descriptors for braces `{ ... }`, doc comments `/** ... */`, block comments `/* ... */`, and multi-line import/include blocks.

## Scalability & Anti-Hardcoding Evaluation
- **Is it hardcoded?**: No. Inlay hints dynamically match resolved signature parameters to arguments; structure view dynamically walks the PSI hierarchy.
- **Is it high performance?**: Yes. Inlay collectors run incrementally in background threads without interfering with typing latency.

## Feature Implementation Map
- Inlay Hints: [`CompactInlayHintsProvider.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/editor/CompactInlayHintsProvider.java)
- Structure View: [`CompactStructureViewFactory.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/structure/CompactStructureViewFactory.java), [`CompactStructureViewModel.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/structure/CompactStructureViewModel.java)
- Breadcrumbs: [`CompactBreadcrumbsProvider.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/editor/CompactBreadcrumbsProvider.java)
- Code Folding: [`CompactFoldingBuilder.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/editor/CompactFoldingBuilder.java)
- Registration: [`plugin.xml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/resources/META-INF/plugin.xml) (`<codeInsight.declarativeInlayProvider>`, `<lang.psiStructureViewFactory>`, `<breadcrumbsInfoProvider>`, `<lang.foldingBuilder>`)
- Unit Tests:
  - [`CompactInlayHintsProviderTest.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/editor/CompactInlayHintsProviderTest.java)
  - [`CompactStructureViewTest.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/structure/CompactStructureViewTest.java)
  - [`CompactBreadcrumbsProviderTest.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/editor/CompactBreadcrumbsProviderTest.java)
  - [`CompactFoldingBuilderTest.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/editor/CompactFoldingBuilderTest.java)

## Consequences & Future Maintenance
- When new declaration types or control-flow blocks are introduced, register their icons in `CompactStructureViewModel` and block ranges in `CompactFoldingBuilder`.
